/**
 *
 */
package nablarch.fw.messaging.handler;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.*;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.Handler;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.reader.StructuredFwHeaderDefinition;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

import nablarch.fw.web.servlet.ServletExecutionContext;

import nablarch.test.core.log.LogVerifier;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.web.servlet.MockServletContext;
import nablarch.test.support.web.servlet.MockServletRequest;
import nablarch.test.support.web.servlet.MockServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * {@link HttpMessagingResponseBuildingHandler}のテストを行います。
 *
 * @author TIS
 */
public class HttpMessagingResponseBuildingHandlerTest {

    @Rule
    public TestName testNameRule = new TestName();

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("nablarch/fw/messaging/handler/HttpMessagingDataParseHandlerTest.xml");

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        String requestId = testNameRule.getMethodName();
        ThreadContext.setRequestId(requestId);
        OnMemoryLogWriter.clear();
    }

    /**
     * フォーマットファイル名を取得します。
     * @param formatName フォーマット名
     * @return フォーマットファイル名
     */
    private String getFormatFileName(String formatName) {
        FilePathSetting fps = FilePathSetting.getInstance();
        return Builder.concat(
                   fps.getBasePathSettings().get("format").getPath(),
                   "/", formatName, ".",
                   fps.getFileExtensions().get("format")
               );

    }

    /**
     * 実行コンテキストを作成します。
     * @param requestId リクエストID
     * @param requestStream 要求データのストリーム
     * @param messageIdHeader ヘッダに設定するmessageId
     * @param correlationIdHeader ヘッダに設定するcorrelationId
     * @return 実行コンテキスト
     */
    private ServletExecutionContext createExecutionContext(String requestId, InputStream requestStream, String messageIdHeader, String correlationIdHeader) {
        MockServletRequest servletRequest = new MockServletRequest();
        MockServletResponse servletResponse = new MockServletResponse();
        MockServletContext servletContext = new MockServletContext();

        servletRequest.setInputStream(requestStream);
        if (messageIdHeader != null) {
            servletRequest.addHeader("X-Message-Id", messageIdHeader);
        }
        if (correlationIdHeader != null) {
            servletRequest.addHeader("X-Correlation-Id", correlationIdHeader);
        }

        ServletExecutionContext context = new ServletExecutionContext(servletRequest, servletResponse, servletContext);
        HttpRequest request = context.getHttpRequest();
        request.setRequestUri(Builder.concat("/action/", requestId));
        request.setRequestPath(request.getRequestUri());

        return context;
    }

    /**
     * フォーマッタを使用して 応答データの構築を行います。
     *
     * @param expectedXml 応答データの元
     * @param encode 応答データのエンコーディング
     * @param formatfile 応答データのフォーマット定義ファイル
     * @return 応答データ
     */
    private String createResponseData(String expectedXml, String encode, File formatfile) throws Exception {
        DataRecordFormatter formatter = FormatterFactory.getInstance().createFormatter(formatfile).initialize();
        formatter.setInputStream(new ByteArrayInputStream(expectedXml.getBytes(encode)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.setOutputStream(baos);
        formatter.setInputStream(new ByteArrayInputStream(expectedXml.getBytes(encode)));
        formatter.writeRecord(formatter.readRecord());
        return new String(baos.toByteArray(), encode);
    }

    /**
     * テスト用のハンドラを設定します。
     * @param requestId リクエストID
     * @param requestData 要求データ
     * @param responseData 応答データ
     * @param statusCodeHeader ステータスコード
     * @return
     */
    private ServletExecutionContext setupContext(String requestId, byte[] requestData, Object responseData, String statusCodeHeader) {
        return setupContext(requestId, requestData, responseData, statusCodeHeader, Long.toString(System.currentTimeMillis()), null);
    }

    /**
     * テスト用のハンドラを設定します。
     * @param requestId リクエストID
     * @param requestData 要求データ
     * @param responseData 応答データ
     * @param statusCodeHeader ステータスコード
     * @param messageIdHeader ヘッダに設定するmessageId
     * @return
     */
    private ServletExecutionContext setupContext(String requestId, byte[] requestData, Object responseData, String statusCodeHeader, String messageIdHeader) {
        return setupContext(requestId, requestData, responseData, statusCodeHeader, messageIdHeader, null);
    }

    /**
     * テスト用のハンドラを設定します。
     * @param requestId リクエストID
     * @param requestData 要求データ
     * @param responseData 応答データ
     * @param statusCodeHeader ステータスコード
     * @param messageIdHeader ヘッダに設定するmessageId
     * @param correlationIdHeader ヘッダに設定するcorrelationId
     * @return
     */
    private ServletExecutionContext setupContext(String requestId, byte[] requestData, final Object responseData, final String statusCodeHeader, String messageIdHeader, String correlationIdHeader) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(requestData);
        ServletExecutionContext context = createExecutionContext(requestId, inputStream, messageIdHeader, correlationIdHeader);

        //テスト対象のハンドラを生成。
        FwHeaderDefinition fwHeaderDefinition = SystemRepository.get("fwHeaderDefinition");
        HttpMessagingRequestParsingHandler parserHandler = new HttpMessagingRequestParsingHandler();
        parserHandler.setBodyLengthLimit(500);
        HttpMessagingResponseBuildingHandler builderHandler = new HttpMessagingResponseBuildingHandler();
        if(fwHeaderDefinition != null){
            //システムリポジトリに存在していれば設定する。
            //(デフォルトのフレームワーク制御ヘッダを使うテストのための分岐)
        	parserHandler.setFwHeaderDefinition(fwHeaderDefinition);
        	builderHandler.setFwHeaderDefinition(fwHeaderDefinition);
        }

        context
            .addHandler(parserHandler)
            .addHandler(builderHandler)
            .addHandler(new Handler<RequestMessage, ResponseMessage>() {
                public ResponseMessage handle(RequestMessage data, nablarch.fw.ExecutionContext context) {
                    if (responseData == null) {
                        if (statusCodeHeader == null) {
                            return data.reply();
                        } else {
                            return data.reply().setStatusCodeHeader(statusCodeHeader);
                        }
                    } else {
                        if (statusCodeHeader == null) {
                            return data.reply().addRecord(responseData);
                        } else {
                            return data.reply().addRecord(responseData).setStatusCodeHeader(statusCodeHeader);
                        }
                    }
                }
            });
        return context;
    }

    /**
     * 正常系のXML受信テストを行います。<br>
     *
     * 条件：<br>
     *   制御ヘッダ、業務データがそれぞれ存在し、XMLとしての構文も問題ない電文を受信する。<br>
     *
     * 期待結果：<br>
     *   業務アクションで作成された応答データが正常に電文化されること。<br>
     *   業務アクションで設定されたステータスコードが返却されること。<br>
     *   応答電文に制御ヘッダが埋め込まれること。<br>
     */
    @Test
    public void testNormalXml() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "  <request>\n" +
                "    <_nbctlhdr>\n" +
                "      <userId>unitTest</userId>\n" +
                "      <resendFlag>0</resendFlag>\n" +
                "    </_nbctlhdr>\n" +
                "    <user>\n" +
                "      <id>nablarch</id>\n" +
                "      <name>ナブラーク</name>\n" +
                "      <cno>1234567890123456</cno>\n" +
                "    </user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 想定される応答電文
        String responseTelegram =
                "<response>\n" +
                "  <_nbctlhdr>\n" +
                "    <statusCode>200</statusCode>\n" +
                "  </_nbctlhdr>\n" +
                "  <result>\n" +
                "    <msg>succes\\ns</msg>\n" +
                "  </result>\n" +
                "</response>";

        // 応答電文フォーマット
        File responseFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_SEND")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", "succes\\ns");

        LogVerifier.setExpectedLogMessages(createExpectedLogMessages(requestTelegram, createResponseData(responseTelegram, "UTF-8", responseFormatFile)));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, resObj, "200");
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(200, response.getStatusCode());
        assertEquals(createResponseData(responseTelegram, "UTF-8", responseFormatFile),
                response.getBodyString());

        assertEquals("電文読み込み後もリクエストIDは不変であること。", requestId, ThreadContext.getRequestId());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("障害通知ログは出力されないこと\nlogs = [" + logs + ']', logs.isEmpty(), is(true));
    }

    /**
     * 正常系のJSON受信テストを行います。
     *
     * 条件：<br>
     *   制御ヘッダ、業務データがそれぞれ存在し、JSONとしての構文も問題ない電文を受信する。<br>
     *
     * 期待結果：<br>
     *   業務アクションで作成された応答データが正常に電文化されること。<br>
     *   業務アクションで設定されたステータスコードが返却されること。<br>
     *   応答電文に制御ヘッダが埋め込まれること。<br>
     */
    @Test
    public void testNormalJson() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "{\n" +
                "  \"_nbctlhdr\":{\n" +
                "    \"userId\":\"unitTest\",\n" +
                "    \"resendFlag\":\"0\"\n" +
                "  }\n" +
                "  \"user\":{\n" +
                "    \"id\":\"nablarch\",\n" +
                "    \"name\":\"ナブラーク\"\n" +
                "  }\n" +
                "}";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 想定される応答電文
        String responseTelegram =
                "{\n" +
                "  \"_nbctlhdr\":{\n" +
                "    \"statusCode\":\"200\"\n" +
                "  }\n" +
                "  \"result\":{\n" +
                "    \"msg\":\" success \\n\"\n" +
                "  }\n" +
                "}";

        // 応答電文フォーマット
        File responseFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_SEND")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", " success \n");

        // 期待値
        LogVerifier.setExpectedLogMessages(createExpectedLogMessages(requestTelegram, createResponseData(responseTelegram, "UTF-8", responseFormatFile)));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, resObj, "200");
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(200, response.getStatusCode());
        assertEquals(createResponseData(responseTelegram, "UTF-8", responseFormatFile),
                response.getBodyString());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("障害通知ログは出力されないこと\nlogs = [" + logs + ']', logs.isEmpty(), is(true));
    }

    /**
     * 制御ヘッダがない電文の受信テストを行います。<br>
     *
     * 条件：<br>
     *   業務データのみの電文を受信する。<br>
     *
     * 期待結果：<br>
     *   業務アクションで作成された応答データが正常に電文化されること。<br>
     *   業務アクションで設定されたステータスコードが返却されること。<br>
     */
    @Test
    public void testNoControlHeader() throws Exception {
        // 現在のFW制御ヘッダ定義を取得する
        StructuredFwHeaderDefinition headerDef = SystemRepository.get("fwHeaderDefinition");
        Field fr = headerDef.getClass().getDeclaredField("fwHeaderKeys");
        fr.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> readFwHeaderKeys = (Map<String, String>)fr.get(headerDef);

        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "  <user>\n" +
                "    <id>nablarch</id>\n" +
                "    <name>ナブラーク</name>\n" +
                "  </user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 想定される応答電文
        String responseTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <result>\n" +
                "    <msg>success</msg>\n" +
                "  </result>\n" +
                "</response>";

        // 応答電文フォーマット
        File responseFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_SEND")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", "success");
        String statusCodeHeader = "200";

        // 現在のFW制御ヘッダ定義を上書きする
        headerDef.setFwHeaderKeys(null);

        // 期待値
        LogVerifier.setExpectedLogMessages(createExpectedLogMessages(requestTelegram, createResponseData(responseTelegram, "UTF-8", responseFormatFile)));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, resObj, statusCodeHeader);
        HttpResponse response = context.handleNext(context.getHttpRequest());

        // FW制御ヘッダ定義を戻す
        headerDef.setFwHeaderKeys(readFwHeaderKeys);

        assertEquals(200, response.getStatusCode());
        assertEquals(createResponseData(responseTelegram, "UTF-8", responseFormatFile), response.getBodyString());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("障害通知ログは出力されないこと\nlogs = [" + logs + ']', logs.isEmpty(), is(true));

    }

    /**
     * 関連メッセージIDが設定されているリクエストの受信テストを行います。<br>
     *
     * 条件：<br>
     *   関連メッセージIDが設定されているリクエストを受信する。<br>
     *
     * 期待結果：<br>
     *   業務アクションで作成された応答データが正常に電文化されること。<br>
     *   業務アクションで設定されたステータスコードが返却されること。<br>
     *   応答電文に制御ヘッダが埋め込まれること。<br>
     */
    @Test
    public void testIncludeCorrelationMessageId() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "  <_nbctlhdr>\n" +
                "    <userId>unitTest</userId>\n" +
                "    <resendFlag>0</resendFlag>\n" +
                "  </_nbctlhdr>\n" +
                "  <user>\n" +
                "    <id>nablarch</id>\n" +
                "    <name>ナブラーク</name>\n" +
                "  </user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 想定される応答電文
        String responseTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <_nbctlhdr>\n" +
                "    <statusCode>200</statusCode>\n" +
                "  </_nbctlhdr>\n" +
                "  <result>\n" +
                "    <msg>success</msg>\n" +
                "  </result>\n" +
                "</response>";

        // 応答電文フォーマット
        File responseFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_SEND")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", "success");

        // 期待値
        LogVerifier.setExpectedLogMessages(createExpectedLogMessages(requestTelegram, createResponseData(responseTelegram, "UTF-8", responseFormatFile)));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, resObj, "200", "messageId",
                "correlationId");
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(200, response.getStatusCode());
        assertEquals(createResponseData(responseTelegram, "UTF-8", responseFormatFile), response.getBodyString());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("ログは出力されないこと\nlogs = [" + logs + ']', logs.isEmpty(), is(true));

    }

    /**
     * 応答データにステータスコードが設定されていない場合のテストを行います。<br>
     *
     * 条件：<br>
     *   業務アクションでステータスコードが設定されていない。<br>
     *
     * 期待結果：<br>
     *   業務アクションで作成された応答データが正常に電文化されること。<br>
     *   ステータスコード200(OK)が返却されること。<br>
     */
    @Test
    public void testNoStatusCode() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "  <_nbctlhdr>\n" +
                "    <userId>unitTest</userId>\n" +
                "    <resendFlag>0</resendFlag>\n" +
                "  </_nbctlhdr>\n" +
                "  <user>\n" +
                "    <id>nablarch</id>\n" +
                "    <name>ナブラーク</name>\n" +
                "  </user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 想定される応答電文
        String responseTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <result>\n" +
                "    <msg>success</msg>\n" +
                "  </result>\n" +
                "</response>";

        // 応答電文フォーマット
        File responseFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_SEND")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", "success");

        // 期待値
        LogVerifier.setExpectedLogMessages(createExpectedLogMessages(requestTelegram, createResponseData(responseTelegram, "UTF-8", responseFormatFile)));

        // テスト実施
        ServletExecutionContext context = setupContext(
                requestId, requestData, resObj, null, "messageId", "correlationId");
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(200, response.getStatusCode());
        assertEquals(createResponseData(responseTelegram, "UTF-8", responseFormatFile), response.getBodyString());

        LogVerifier.verify("messaging log assertion failed.");


        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("障害通知ログは出力されないこと\nlogs = [" + logs + ']', logs.isEmpty(), is(true));
    }

    /**
     * 応答データにステータスコードが設定されていない場合のテストを行います。<br>
     *
     * 条件：<br>
     *   業務アクションでステータスコードが設定されていない。<br>
     *
     * 期待結果：<br>
     *   業務アクションで作成された応答データが正常に電文化されること。<br>
     *   ステータスコード200(OK)が返却されること。<br>
     */
    @Test
    public void testNoResultTelegram() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "  <_nbctlhdr>\n" +
                "    <userId>unitTest</userId>\n" +
                "    <resendFlag>0</resendFlag>\n" +
                "  </_nbctlhdr>\n" +
                "  <user>\n" +
                "    <id>nablarch</id>\n" +
                "    <name>ナブラーク</name>\n" +
                "  </user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = null;

        // 期待値
        LogVerifier.setExpectedLogMessages(createReceivedOnlyExpectedLogMessages(requestTelegram));

        // テスト実施
        ServletExecutionContext context = setupContext(
                requestId, requestData, resObj, "400", "messageId", "correlationId");
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(400, response.getStatusCode());
        assertEquals(0, response.getBodyStream().available());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("ログは出力されないこと\nlogs = [" + logs + ']', logs.isEmpty(), is(true));
    }

    /**
     * 業務エラー想定のXML受信テストを行います。<br>
     *
     * 条件：<br>
     *   制御ヘッダ、業務データがそれぞれ存在し、XMLとしての構文も問題ない電文を受信する。<br>
     *   業務クラスにてステータスコード"400"(エラー想定)を設定する
     *
     * 期待結果：<br>
     *   業務アクションで作成された応答データが正常に電文化されること。<br>
     *   業務アクションで設定されたステータスコードが返却されること。<br>
     *   応答電文に制御ヘッダが埋め込まれること。<br>
     */
    @Test
    public void testAbnormalRequest() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "  <_nbctlhdr>\n" +
                "    <userId>unitTest</userId>\n" +
                "    <resendFlag>0</resendFlag>\n" +
                "  </_nbctlhdr>\n" +
                "  <user>\n" +
                "    <id>nablarch</id>\n" +
                "    <name>ナブラーク</name>\n" +
                "  </user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 想定される応答電文
        String responseTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <_nbctlhdr>\n" +
                "    <statusCode>400</statusCode>\n" +
                "  </_nbctlhdr>\n" +
                "  <result>\n" +
                "    <msg>fail</msg>\n" +
                "  </result>\n" +
                "</response>";

        // 応答電文フォーマット
        File responseFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_SEND")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", "fail");

        // 期待値
        LogVerifier.setExpectedLogMessages(createExpectedLogMessages(requestTelegram, createResponseData(responseTelegram, "UTF-8", responseFormatFile)));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, resObj, "400");
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(400, response.getStatusCode());
        assertEquals(createResponseData(responseTelegram, "UTF-8", responseFormatFile), response.getBodyString());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        List<String> infoLog = new ArrayList<String>();
        for (String log : logs) {
            if (log.contains("INFO")) {
                infoLog.add(log);
            }
        }
        assertThat("ログは出力されないこと\ninfoLog = [" + infoLog + ']', infoLog.isEmpty(), is(true));
    }

    /**
     * 標準的なフォーマッタサポート用抽象クラス({@link DataRecordFormatterSupport})を使用しないフォーマッタを使用した場合のテストを行います。<br>
     *
     * 条件：<br>
     *   単純に{@link DataRecordFormatter}を実装しただけのテスト用フォーマッタを使用する。<br>
     *
     * 期待結果：<br>
     *   結果に含まれるmimeTypeや文字コードセットがデフォルトのものとなること。<br>
     */
    @Test
    public void testNotUsingDefaultSupportFormatter() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "  <_nbctlhdr>\n" +
                "    <userId>unitTest</userId>\n" +
                "    <resendFlag>0</resendFlag>\n" +
                "  </_nbctlhdr>\n" +
                "  <user>\n" +
                "    <id>nablarch</id>\n" +
                "    <name>ナブラーク</name>\n" +
                "  </user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 想定される応答電文
        String responseTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <_nbctlhdr>\n" +
                "    <statusCode>200</statusCode>\n" +
                "  </_nbctlhdr>\n" +
                "  <result>\n" +
                "    <msg>success</msg>\n" +
                "  </result>\n" +
                "</response>";

        // 応答電文フォーマット
        File responseFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_SEND")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", "success");

        // 期待値
        LogVerifier.setExpectedLogMessages(createReceivedOnlyExpectedLogMessages(requestTelegram));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, resObj, "200");
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(200, response.getStatusCode());
        assertEquals(0, response.getBodyStream().available());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("障害通知ログは出力されないこと\nlogs = [" + logs + ']', logs.isEmpty(), is(true));
    }


    /**
     * FW制御ヘッダ定義が設定されていない場合のテストを行います。<br>
     *
     * 条件：<br>
     *   設定ファイルにFW制御ヘッダ定義が設定されていない<br>
     *
     * 期待結果：<br>
     *   デフォルトのFW制御ヘッダ定義(構造化データ用定義でFWヘッダキーなし)でデータが読み取られること<br>
     */
    @Test
    public void testNoFwHeaderDef() throws Exception {
        // 今回用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/fw/messaging/handler/HttpMessagingDataParseHandlerTestNoHeaderDef.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);

        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "  <_nbctlhdr>\n" +
                "    <userId>unitTest</userId>\n" +
                "    <resendFlag>0</resendFlag>\n" +
                "  </_nbctlhdr>\n" +
                "  <user>\n" +
                "    <id>nablarch</id>\n" +
                "    <name>ナブラーク</name>\n" +
                "  </user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 想定される応答電文
        String responseTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <result>\n" +
                "    <msg>success</msg>\n" +
                "  </result>\n" +
                "</response>";

        // 応答電文フォーマット
        File responseFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_SEND")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", "success");
        String statusCodeHeader = "200";

        // 期待値
        String expectedResponseTelegram = createResponseData(responseTelegram, "UTF-8", responseFormatFile);

        LogVerifier.setExpectedLogMessages(createExpectedLogMessages(requestTelegram, expectedResponseTelegram));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, resObj, statusCodeHeader);
        HttpResponse response = context.handleNext(context.getHttpRequest());

        // テスト用のリポジトリ構築にもどす
        loader = new XmlComponentDefinitionLoader(
                "nablarch/fw/messaging/handler/HttpMessagingDataParseHandlerTest.xml");
        container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);
        OnMemoryLogWriter.clear();

        assertEquals(200, response.getStatusCode());
        assertEquals(expectedResponseTelegram, response.getBodyString());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("障害通知ログは出力されないこと\nlogs = [" + logs + ']', logs.isEmpty(), is(true));

    }




    public static class TestFormatterFactory extends FormatterFactory {
        @Override
        protected DataRecordFormatter createFormatter(String fileType,
                String formatFilePath) {
            if (fileType.equals("Hoge")) {
                return new HogeFormatter();
            }
            return super.createFormatter(fileType, formatFilePath);
        }
    }
    public static class HogeFormatter implements DataRecordFormatter {
        @Override
        public DataRecord readRecord() throws IOException,
                InvalidDataFormatException {
            return null;
        }
        @Override
        public void writeRecord(Map<String, ?> record) throws IOException,
                InvalidDataFormatException {
        }
        @Override
        public void writeRecord(String recordType, Map<String, ?> record)
                throws IOException, InvalidDataFormatException {
        }
        @Override
        public DataRecordFormatter initialize() {
            return this;
        }
        @Override
        public DataRecordFormatter setInputStream(InputStream stream) {
            return this;
        }
        @Override
        public void close() {
        }

        @Override
        public DataRecordFormatter setDefinition(LayoutDefinition definition) {
            return this;
        }
        @Override
        public DataRecordFormatter setOutputStream(OutputStream stream) {
            return this;
        }
        @Override
        public boolean hasNext() throws IOException {
            return false;
        }

        @Override
        public int getRecordNumber() {
            return 0;
        }
    }

    /***
     * ログアサート用のリストを作成します。
     * 出力されるログはデフォルトのフォーマッタで整形されていることを前提としています。
     *
     * @param expectedRequestBody  要求電文
     * @param expectedResponseBody 応答電文
     */
    private List<Map<String, String>> createExpectedLogMessages(
            String expectedRequestBody, String expectedResponseBody) {

        Map<String, String> receivedLogMessage = new HashMap<String, String>();
        receivedLogMessage.put("logLevel", "INFO");
        receivedLogMessage.put("message1", "@@@@ HTTP RECEIVED MESSAGE @@@@");
        receivedLogMessage.put("message2", "message_body   = [" + expectedRequestBody + "]");

        Map<String, String> sentLogMessage = new HashMap<String, String>();
        sentLogMessage.put("logLevel", "INFO");
        sentLogMessage.put("message1", "@@@@ HTTP SENT MESSAGE @@@@");
        sentLogMessage.put("message2", "message_body   = [" + expectedResponseBody + "]");

        List<Map<String, String>> expectedLogMessages = new ArrayList<Map<String,String>>();
        expectedLogMessages.add(receivedLogMessage);
        expectedLogMessages.add(sentLogMessage);

        return expectedLogMessages;
    }

    /***
     * ログアサート用のリストを作成します。
     * 出力されるログはデフォルトのフォーマッタで整形されていることを前提としています。
     *
     * @param expectedRequestBody  要求電文
     */
    private List<Map<String, String>> createReceivedOnlyExpectedLogMessages(String expectedRequestBody) {

        Map<String, String> receivedLogMessage = new HashMap<String, String>();
        receivedLogMessage.put("logLevel", "INFO");
        receivedLogMessage.put("message1", "@@@@ HTTP RECEIVED MESSAGE @@@@");
        receivedLogMessage.put("message2", "message_body   = [" + expectedRequestBody + "]");

        List<Map<String, String>> expectedLogMessages = new ArrayList<Map<String,String>>();
        expectedLogMessages.add(receivedLogMessage);

        return expectedLogMessages;
    }

}
