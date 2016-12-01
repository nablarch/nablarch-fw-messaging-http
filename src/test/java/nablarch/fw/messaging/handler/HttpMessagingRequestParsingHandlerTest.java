/**
 *
 */
package nablarch.fw.messaging.handler;

import nablarch.core.ThreadContext;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.FwHeaderDefinition;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * {@link HttpMessagingRequestParsingHandler}のテストを行います。
 *
 * @author TIS
 */
public class HttpMessagingRequestParsingHandlerTest {

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

        if(fwHeaderDefinition != null){
            //システムリポジトリに存在していれば設定する。
            //(デフォルトのフレームワーク制御ヘッダを使うテストのための分岐)
            parserHandler.setFwHeaderDefinition(fwHeaderDefinition);
        }

        context
                .addHandler(parserHandler);
        return context;
    }

    /**
     * 電文ボディがないリクエストの受信テストを行います。<br>
     *
     * 条件：<br>
     *   電文ボディがないリクエストを受信する。<br>
     *
     * 期待結果：<br>
     *   ステータスコード400(BadRequest)が返却されること。<br>
     */
    @Test
    public void testNoContents() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 要求データ
        byte[] requestData = new byte[]{};

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, null, null);

        HttpResponse response = context.handleNext(context.getHttpRequest());

        // 期待値

        assertEquals(400, response.getStatusCode());

        List<String> logs = OnMemoryLogWriter.getMessages("writer.appLog");
        List<String> infoLogs = filterLog(logs, "INFO");
        assertThat("INFOログが出力されていること", infoLogs.size(), is(1));
        assertThat(logs.get(0), allOf(containsString("INFO"), containsString("invalid request message received.")));

    }

    /**
     * メッセージIDが設定されていないリクエストの受信テストを行います。<br>
     *
     * 条件：<br>
     *   メッセージIDが設定されていないリクエストを受信する。<br>
     *
     * 期待結果：<br>
     *   ステータスコード400(BadRequest)が返却されること。<br>
     */
    @Test
    public void testNoMessageId() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "<_nbctlhdr>\n" +
                "<userId>unitTest</userId>\n" +
                "<resendFlag>0</resendFlag>\n" +
                "</_nbctlhdr>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "</request>";

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, null, null, null);
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(400, response.getStatusCode());

        List<String> logs = filterLog(OnMemoryLogWriter.getMessages("writer.appLog"), "INFO");
        assertThat("INFOレベルのログが出力されていること", logs.size(), is(1));
        assertThat(logs.get(0), allOf(containsString("INFO"), containsString("Request of empty messageId received.")));
    }

    /**
     * BODYのサイズ超過エラー。<br>
     *
     * 条件：<br>
     *   ボディのサイズ上限を設定する。<br>
     *
     * 期待結果：<br>
     *   サイズ上限エラーが発生すること。<br>
     */
    @Test
    public void testSizeLimitOverRequest() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "<_nbctlhdr>\n" +
                "<userId>unitTest</userId>\n" +
                "<resendFlag>0</resendFlag>\n" +
                "</_nbctlhdr>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // 期待値
        LogVerifier.setExpectedLogMessages(createReceivedOnlyExpectedLogMessages(requestTelegram));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, null, null);
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(413, response.getStatusCode());

        List<String> logs = filterLog(OnMemoryLogWriter.getMessages("writer.appLog"), "INFO");
        assertThat("INFOレベルのログが出力されていること", logs.size(), is(1));
        assertThat(logs.get(0), allOf(containsString("INFO"), containsString("request entity too large.")));
    }

    /**
     * BODYのサイズ範囲外エラー。<br>
     *
     * 条件：<br>
     *   ボディサイズに負の値を設定する。<br>
     *
     * 期待結果：<br>
     *   不正エラーが発生すること。<br>
     */
    @Test
    public void testInvalidSizeLimit() throws Exception {

        try {
            // テスト用にハンドラーの設定を書き換え
            HttpMessagingRequestParsingHandler parserHandler = new HttpMessagingRequestParsingHandler();
            parserHandler.setBodyLengthLimit(-1);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("bodyLengthLimit must not be negative"));
        }
    }

    /**
     * 不正な制御ヘッダが設定されている電文の受信テストを行います。<br>
     *
     * 条件：<br>
     *   不正な制御ヘッダが設定されている電文を受信する。<br>
     *
     * 期待結果：<br>
     *   ステータスコード400(BadRequest)が返却されること。<br>
     */
    @Test
    public void testAbnormalControlHeader() throws Exception {
        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "<_NGHeader>\n" +
                "<userId>unitTest</userId>\n" +
                "<resendFlag>0</resendFlag>\n" +
                "</_NGHeader>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "</request>";

        // 要求電文フォーマット
        File requestFormatFile = new File(getFormatFileName(Builder.concat(requestId, "_RECEIVE")));

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // 期待値
        LogVerifier.setExpectedLogMessages(createReceivedOnlyExpectedLogMessages(requestTelegram));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, null, null);
        HttpResponse response = context.handleNext(context.getHttpRequest());

        assertEquals(400, response.getStatusCode());

        LogVerifier.verify("messaging log assertion failed.");

        List<String> logs = filterLog(OnMemoryLogWriter.getMessages("writer.appLog"), "INFO");
        assertThat("INFOレベルのログが出力されていること", logs.size(), is(1));
        assertThat(logs.get(0), allOf(containsString("INFO"), containsString("invalid format request message received.")));
    }

    /**
     * サーブレットコンテキスト上で動作していない場合のテストを行います。<br>
     *
     * 条件：<br>
     *   new ExecutionContext()した実行コンテキストでハンドラを実行する。<br>
     *
     * 期待結果：<br>
     *   ServletExecutionContext以外は想定しないため、ClassCastExceptionが送出されること。
     *   ※アプリケーション経由で来た場合は、ServletExecutionContext以外はありえない。
     */
    @Test(expected = ClassCastException.class)
    public void testOnInvalidExecutionContext() {
        ExecutionContext ctx = new ExecutionContext()
                .addHandler(new HttpMessagingRequestParsingHandler());

        // リクエストID
        String requestId = ThreadContext.getRequestId();
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{});
        ServletExecutionContext srvctx = createExecutionContext(requestId, bais, "msgid", null);

        // テスト実施
        ctx.handleNext(srvctx.getHttpRequest());
    }

    /**
     * 要求電文のフォーマット定義ファイルが存在しない場合のテストを行います。<br>
     *
     * 条件：<br>
     *   要求電文のフォーマット定義ファイルが存在しない<br>
     *
     * 期待結果：<br>
     *   例外が送出されること。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNoRequestFormatDef() throws Exception {

        // リクエストID
        String requestId = ThreadContext.getRequestId();

        // 要求電文
        String requestTelegram =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "<_nbctlhdr>\n" +
                "<userId>unitTest</userId>\n" +
                "<resendFlag>0</resendFlag>\n" +
                "</_nbctlhdr>\n" +
                "<user>\n" +
                "<id>nablarch</id>\n" +
                "<name>ナブラーク</name>\n" +
                "</user>\n" +
                "</request>";

        // 要求データ
        byte[] requestData = requestTelegram.getBytes("UTF-8");

        // ダミー業務クラスで返却する応答データ
        Map<String, Object> resObj = new HashMap<String, Object>();
        resObj.put("result.msg", "success");

        // 期待値
        LogVerifier.setExpectedLogMessages(createReceivedOnlyExpectedLogMessages(requestTelegram));

        // テスト実施
        ServletExecutionContext context = setupContext(requestId, requestData, resObj, "200");
        context.handleNext(context.getHttpRequest());
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

    private List<String> filterLog(List<String> logs, String condition) {
        List<String> results = new ArrayList<String>();
        for (String log : logs) {
            if (log.contains(condition)) {
                results.add(log);
            }
        }
        return results;
    }
}

