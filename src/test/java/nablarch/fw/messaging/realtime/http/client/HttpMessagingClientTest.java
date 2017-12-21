package nablarch.fw.messaging.realtime.http.client;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.messaging.MessageSenderSettings;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.messaging.realtime.http.client.HttpProtocolClient.HttpRequestMethodEnum;
import nablarch.fw.messaging.realtime.http.dto.HttpResult;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingInvalidDataFormatException;
import nablarch.fw.messaging.realtime.http.streamio.HttpInputStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.HttpOutputStreamWriter;
import nablarch.test.core.log.LogVerifier;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.tool.Hereis;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


/**
 * {@link ProtocolLientTestHttpMessagingClient}のテスト。<br>
 * ただし、コネクションは張らない(スタブを用いる)。
 * 
 * @author Masaya Seko
 */
public class HttpMessagingClientTest {

    @Rule
    public SystemRepositoryResource resource = new SystemRepositoryResource("nablarch/fw/messaging/realtime/http/client/HttpMessagingClientTest.xml");

    public void initRepository(String suffix) {
        SystemRepository.clear();
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                this.getClass().getName().replace('.', '/') + suffix + ".xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }

    private String getFormatFileName(String formatName) {
        return FilePathSetting.getInstance().getBasePathSettings().get("format").getPath() +
                "/" + formatName + "." + 
                FilePathSetting.getInstance().getFileExtensions().get("format");
        
    }

    /**
     * HttpMessagingClientはデフォルトではHttpProtocolBasicClientを使用すること。
     */
    @Test
    public void testCreateHttpProtocolClient() {
        ProtocolTestHttpMessagingClient client = new ProtocolTestHttpMessagingClient();
        assertThat(client.getDefaultHttpProtocolClient().getClass().getName(), is("nablarch.fw.messaging.realtime.http.client.HttpProtocolBasicClient"));
    }

    class ProtocolTestHttpMessagingClient extends HttpMessagingClient {
        HttpProtocolClient getDefaultHttpProtocolClient(){
            return createHttpProtocolClient();
        }
    }
    

    /***
     * GETメソッドで通信を行えること。
     */
    @Test
    public void testGetMessage() {
        //ユニットテスト用フォーマット定義ファイル準備
        prepareFormatFileGetMessage();

        SyncMessage requestMessage = null;
        SyncMessage reciveMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;

        //GET
        requestMessage = new SyncMessage("RM21AB0100");
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
                        ArrayList<String> arrayList = null;
                        arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        map.put("Content-Type", arrayList);
                        arrayList = new ArrayList<String>();
                        arrayList.add("HTTP/1.1 200 OK");
                        map.put(null, arrayList);
                        httpResult.setHeaderInfo(map);
                        httpResult.setResponseCode(200);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"O \\nK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        client.setQueryStringEncoding("UTF-8");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "",
                        "GET http://localhost:8090/rm21ab0100",
                        "{\"messageCode\":\"100\", \"message\":\"O \\nK\"}",
                        200));
        
        reciveMessage = client.sendSync(settings, requestMessage);
        
        assertThat(client.getQueryStringEncoding(), is("UTF-8"));
        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0100"));
        assertThat(client.getLastBodyText(), is(""));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.GET));
        Map<String, Object> headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("200"));
        assertThat((String)(headerRecord.get("Content-Type")), is("application/json; charset=UTF-8"));
        Map<String, Object> dataRecord = reciveMessage.getDataRecord();
        assertThat((String)(dataRecord.get("messageCode")), is("100"));
        assertThat((String)(dataRecord.get("message")), is("O \nK"));

        LogVerifier.verify("messaging log assertion failed.");
    }
    
    /***
     * フォーマット変換エラーの際、例外に想定どおりのパラメータが設定されていること。
     */
    @Test
    public void testGet404() {
        //ユニットテスト用フォーマット定義ファイル準備
        prepareFormatFileGetMessage();

        SyncMessage requestMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;

        //GET
        requestMessage = new SyncMessage("RM21AB0100");
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
                        ArrayList<String> arrayList = null;
                        arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        map.put("Content-Type", arrayList);
                        arrayList = new ArrayList<String>();
                        arrayList.add("HTTP/1.1 404 Not Found");
                        map.put(null, arrayList);
                        httpResult.setHeaderInfo(map);
                        httpResult.setResponseCode(404);
                        String readObject = "404 File not found";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        client.setQueryStringEncoding("UTF-8");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "",
                        "GET http://localhost:8090/rm21ab0100",
                        "404 File not found",
                        404));
        
        try{
            client.sendSync(settings, requestMessage);
            fail();
        }catch(Exception e){
            assertThat(e, is(instanceOf(HttpMessagingInvalidDataFormatException.class)));
            HttpMessagingInvalidDataFormatException formatException = (HttpMessagingInvalidDataFormatException)e;
            assertThat(formatException.getTargetUrl(), is("http://localhost:8090/rm21ab0100"));
            assertThat(formatException.getStatusCode(), is(404));
            assertThat(formatException.getReceiveData(), is("404 File not found"));
            
            LogVerifier.verify("messaging log assertion failed.");
        }
    }

    private void prepareFormatFileGetMessage(){
        File formatFile = null;
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0100_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }

    /***
     * POSTメソッドで通信を行えること。
     */
    @Test
    public void testPostMessage() {
        //ユニットテスト用フォーマット定義ファイル準備
        prepareFormatFilePostMessage();

        SyncMessage requestMessage = null;
        SyncMessage reciveMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;
        Map<String, Object> reqHeaderRecord = null;
        Map<String, Object> requestRecodeData = null;
        Map<String, Object> dataRecord = null;
        Map<String, Object> headerRecord = null;

        //POST(送信Body有。返信にBodyなし)
        requestMessage = new SyncMessage("RM21AB0200");
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
                        ArrayList<String> arrayList = null;
                        arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        map.put("Content-Type", arrayList);
                        arrayList = new ArrayList<String>();
                        arrayList.add("HTTP/1.1 200 OK");
                        map.put(null, arrayList);
                        httpResult.setHeaderInfo(map);
                        httpResult.setResponseCode(200);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "",
                        "POST http://localhost:8090/rm21ab0200",
                        "",
                        200));
        
        reciveMessage = client.sendSync(settings, requestMessage);
        
        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0200"));
        assertThat(client.getLastBodyText(), is(""));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.POST));
        headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("200"));
        
        LogVerifier.verify("messaging log assertion failed.");
        
        //POST(送信、返信にBody有り)
        requestMessage = new SyncMessage("RM21AB0201");
        requestRecodeData = new TreeMap<String, Object>();
        requestRecodeData.put("requestId", "RM21AB0201");
        requestRecodeData.put("firstName", "太郎");
        requestRecodeData.put("lastName", "ナブラ");
        // サロゲートペア対応
        requestRecodeData.put("surrogatepair", "🙀🙀🙀 \n");

        requestMessage.addDataRecord(requestRecodeData);
        reqHeaderRecord = new TreeMap<String, Object>();
        reqHeaderRecord.put("X-xx", null);//意地悪なデータとして、わざとnullを設定する。nullはフレームワーク側で空文字列に置換する。
        requestMessage.setHeaderRecord(reqHeaderRecord);

        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
                        ArrayList<String> arrayList = null;
                        arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        map.put("Content-Type", arrayList);
                        arrayList = new ArrayList<String>();
                        arrayList.add("HTTP/1.1 200 OK");
                        map.put(null, arrayList);
                        httpResult.setHeaderInfo(map);
                        httpResult.setResponseCode(200);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "{\"requestId\":\"RM21AB0201\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\",\"surrogatepair\":\"🙀🙀🙀 \\n\"}",
                        "POST http://localhost:8090/rm21ab0201",
                        "{\"messageCode\":\"100\", \"message\":\"OK\"}",
                        200));
        
        reciveMessage = client.sendSync(settings, requestMessage);
        
        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0201"));
        assertThat(client.getLastBodyText(), is("{\"requestId\":\"RM21AB0201\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\",\"surrogatepair\":\"🙀🙀🙀 \\n\"}"));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.POST));
        headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("200"));
        assertThat((String)(headerRecord.get("Content-Type")), is("application/json; charset=UTF-8"));
        dataRecord = reciveMessage.getDataRecord();
        assertThat((String)(dataRecord.get("messageCode")), is("100"));
        assertThat((String)(dataRecord.get("message")), is("OK"));

        LogVerifier.verify("messaging log assertion failed.");
        
        //POST(SSL設定なし、Proxy設定なし、要求/受信電文差し替えなしを明示的に指定。メッセージID生成は、常にnullを返すように指定)
        requestMessage = new SyncMessage("RM21AB0202");
        requestRecodeData = new TreeMap<String, Object>();
        requestRecodeData.put("requestId", "RM21AB0201");
        requestRecodeData.put("firstName", "太郎");
        requestRecodeData.put("lastName", "ナブラ");
        requestMessage.addDataRecord(requestRecodeData);
        reqHeaderRecord = new TreeMap<String, Object>();
        reqHeaderRecord.put("X-xx", null);//意地悪なデータとして、わざとnullを設定する。nullはフレームワーク側で空文字列に置換する。
        requestMessage.setHeaderRecord(reqHeaderRecord);

        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>();
                        ArrayList<String> arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        treeMap.put("Content-Type", arrayList);
                        httpResult.setHeaderInfo(treeMap);
                        httpResult.setResponseCode(200);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "{\"requestId\":\"RM21AB0201\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\"}",
                        "POST http://localhost:8090/rm21ab0202",
                        "{\"messageCode\":\"100\", \"message\":\"OK\"}",
                        200));
        
        reciveMessage = client.sendSync(settings, requestMessage);
        
        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0202"));
        assertThat(client.getLastBodyText(), is("{\"requestId\":\"RM21AB0201\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\"}"));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.POST));
        headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("200"));
        assertThat((String)(headerRecord.get("Content-Type")), is("application/json; charset=UTF-8"));
        dataRecord = reciveMessage.getDataRecord();
        assertThat((String)(dataRecord.get("messageCode")), is("100"));
        assertThat((String)(dataRecord.get("message")), is("OK"));
        
        LogVerifier.verify("messaging log assertion failed.");
    }

    private void prepareFormatFilePostMessage(){
        File formatFile = null;

        // フォーマットファイル生成(リアルタイム通信　送信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0201_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 requestId X
        2 firstName N
        3 lastName N
        4 surrogatepair N
        *******/
        formatFile.deleteOnExit();
        
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0201_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();


        // フォーマットファイル生成(リアルタイム通信　送信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0202_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 requestId X
        2 firstName N
        3 lastName N
        *******/
        formatFile.deleteOnExit();
        
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0202_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }
    
    /***
     * PUTメソッドで通信を行えること。
     */
    @Test
    public void tesPutMessageBody() {
        //ユニットテスト用フォーマット定義ファイル準備
        prepareFormatFilePutMessage();

        SyncMessage requestMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;
        Map<String, Object> recodeData = null;
        SyncMessage reciveMessage = null;
        Map<String, Object> dataRecord = null;
        Map<String, Object> headerRecord = null;

        //PUT(応答のBody部なし)
        requestMessage = new SyncMessage("RM21AB0300");
        recodeData = new TreeMap<String, Object>();
        recodeData.put("requestId", "RM21AB0300");
        recodeData.put("firstName", "太郎");
        recodeData.put("lastName", "ナブラ");
        requestMessage.addDataRecord(recodeData);
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        HttpResult httpResult = new HttpResult();
                        httpResult.setHeaderInfo(new HashMap<String, List<String>>());
                        httpResult.setResponseCode(200);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "{\"requestId\":\"RM21AB0300\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\"}",
                        "PUT http://localhost:8090/rm21ab0300",
                        "",
                        200));
        
        reciveMessage = client.sendSync(settings, requestMessage);
        
        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0300"));
        assertThat(client.getLastBodyText(), is("{\"requestId\":\"RM21AB0300\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\"}"));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.PUT));
        headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("200"));
        
        LogVerifier.verify("messaging log assertion failed.");


        //PUT(応答のBody部有り)
        requestMessage = new SyncMessage("RM21AB0301");
        recodeData = new TreeMap<String, Object>();
        recodeData.put("requestId", "RM21AB0301");
        recodeData.put("firstName", "太郎");
        recodeData.put("lastName", "ナブラ");
        // サロゲートペア対応
        recodeData.put("surrogatepair", "🙀🙀🙀");
        requestMessage.addDataRecord(recodeData);
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        HttpResult httpResult = new HttpResult();
                        TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>();
                        ArrayList<String> arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        treeMap.put("Content-Type", arrayList);
                        httpResult.setHeaderInfo(treeMap);
                        httpResult.setResponseCode(200);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "{\"requestId\":\"RM21AB0301\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\",\"surrogatepair\":\"🙀🙀🙀\"}",
                        "PUT http://localhost:8090/rm21ab0301",
                        "{\"messageCode\":\"100\", \"message\":\"OK\"}",
                        200));
        
        reciveMessage = client.sendSync(settings, requestMessage);
        
        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0301"));
        assertThat(client.getLastBodyText(), is("{\"requestId\":\"RM21AB0301\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\",\"surrogatepair\":\"🙀🙀🙀\"}"));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.PUT));
        headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("200"));
        dataRecord = reciveMessage.getDataRecord();
        assertThat((String)(dataRecord.get("messageCode")), is("100"));
        assertThat((String)(dataRecord.get("message")), is("OK"));
        
        LogVerifier.verify("messaging log assertion failed.");
    }

    private void prepareFormatFilePutMessage(){
        File formatFile = null;
        // フォーマットファイル生成(リアルタイム通信　送信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0300_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 requestId X
        2 firstName N
        3 lastName N
        *******/
        formatFile.deleteOnExit();

        // フォーマットファイル生成(リアルタイム通信　送信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0301_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 requestId X
        2 firstName N
        3 lastName N
        4 surrogatepair N
        *******/
        formatFile.deleteOnExit();
        
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0301_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }

    /***
     * DELETEメソッドで通信を行えること。
     */
    @Test
    public void tesDeleteMessage() {
        prepareFormatFileDeleteMessage();

        SyncMessage requestMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;
        SyncMessage reciveMessage = null;
        Map<String, Object> dataRecord = null;
        Map<String, Object> headerRecord = null;

        //DELETE(応答のBody部なし)
        requestMessage = new SyncMessage("RM21AB0400");
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        HttpResult httpResult = new HttpResult();
                        httpResult.setHeaderInfo(new HashMap<String, List<String>>());
                        httpResult.setResponseCode(200);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "",
                        "DELETE http://localhost:8090/rm21ab0400",
                        "",
                        200));
        
        reciveMessage = client.sendSync(settings, requestMessage);

        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0400"));
        assertThat(client.getLastBodyText(), is(""));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.DELETE));
        headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("200"));
        
        LogVerifier.verify("messaging log assertion failed.");
        
        
        //DELETE(応答のBody部有り)
        requestMessage = new SyncMessage("RM21AB0401");
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        HttpResult httpResult = new HttpResult();
                        TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>();
                        ArrayList<String> arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        treeMap.put("Content-Type", arrayList);
                        httpResult.setHeaderInfo(treeMap);
                        httpResult.setResponseCode(200);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "",
                        "DELETE http://localhost:8090/rm21ab0401",
                        "{\"messageCode\":\"100\", \"message\":\"OK\"}",
                        200));
        
        reciveMessage = client.sendSync(settings, requestMessage);

        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0401"));
        assertThat(client.getLastBodyText(), is(""));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.DELETE));
        headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("200"));
        dataRecord = reciveMessage.getDataRecord();
        assertThat((String)(dataRecord.get("messageCode")), is("100"));
        assertThat((String)(dataRecord.get("message")), is("OK"));
        
        LogVerifier.verify("messaging log assertion failed.");
    }

    private void prepareFormatFileDeleteMessage(){
        File formatFile = null;
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0401_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }

    /***
     * 不正なデータ(要求電文に必須項目がない場合)を受信した場合。
     */
    @Test
    public void testInvalidRequestBody() {
        initRepository("_invalid1");
        prepareInvalidRequestBodyFormatfile();

        SyncMessage requestMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;

        requestMessage = new SyncMessage("RM21AC0100");
        Map<String, Object> recodeData = new TreeMap<String, Object>();
        recodeData.put("requestId", "RM21AB0201");
        recodeData.put("firstName", "太郎");
        requestMessage.addDataRecord(recodeData);
        
        
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        HttpResult httpResult = new HttpResult();
                        httpResult.setResponseCode(200);
                        return httpResult;
                    }
                };
            }
        };

        client.setUserIdToFormatKey("userId");
        try {
            client.sendSync(settings, requestMessage);
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(HttpMessagingInvalidDataFormatException.class)));
            assertThat(e.getMessage(), is("Invalid request message format. requestId=[RM21AC0100]. URL=[http://localhost:8090/rm21ac0100]."));

            //MessagingExceptionでもキャッチ可能なことを確認
            assertThat(e, is(instanceOf(MessagingException.class)));
        } finally {
            SystemRepository.clear();
        }
    }

    private void prepareInvalidRequestBodyFormatfile(){
        File formatFile = null;
        // フォーマットファイル生成(リアルタイム通信　送信用)
        formatFile = Hereis.file(getFormatFileName("RM21AC0100_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 requestId X
        2 firstName N
        3 lastName N
        *******/
        formatFile.deleteOnExit();

        
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AC0100_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }

    
    /***
     * 不正なデータ(応答電文に必須項目がない場合)を受信した場合。
     */
    @Test
    public void testInvalidResponseBody() {
        initRepository("_invalid1");
        prepareInvalidResponseBodyFormatfile();

        SyncMessage requestMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;

        requestMessage = new SyncMessage("RM21AC0200");
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        HttpResult httpResult = new HttpResult();
                        TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>();
                        ArrayList<String> arrayList = new ArrayList<String>();
                        arrayList.add("Content-Type: application/json; charset=UTF-8");
                        treeMap.put("Content-Type", arrayList);
                        httpResult.setHeaderInfo(treeMap);
                        httpResult.setResponseCode(200);
                        //応答生成
                        String readObject = "{\"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };

        client.setUserIdToFormatKey("userId");
        
        LogVerifier.setExpectedLogMessages(
                createExpectedLogMessages(
                        "",
                        "POST http://localhost:8090/rm21ac0200",
                        "{\"message\":\"OK\"}",
                        200));
        
        try {
            client.sendSync(settings, requestMessage);
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(HttpMessagingInvalidDataFormatException.class)));
            assertThat(e.getMessage(), is("Invalid receive message format. requestId=[RM21AC0200]. URL=[http://localhost:8090/rm21ac0200]. status code=[200]."));
            //MessagingExceptionでもキャッチ可能なことを確認
            assertThat(e, is(instanceOf(MessagingException.class)));
            
            LogVerifier.verify("messaging log assertion failed.");
        } finally {
            SystemRepository.clear();
        }
    }

    private void prepareInvalidResponseBodyFormatfile(){
        File formatFile = null;
        // フォーマットファイル生成(リアルタイム通信　送信用)
        formatFile = Hereis.file(getFormatFileName("RM21AC0200_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [header]
        1 userId X
        *******/
        formatFile.deleteOnExit();

        
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AC0200_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }

    /***
     * 不正なHTTPメソッドを使用しようとした場合。
     */
    @Test
    public void testinvalidHttpMethod() {
        initRepository("_invalid1");

        SyncMessage requestMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;

        requestMessage = new SyncMessage("RM21AC0300");
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        client = new StubHTTPMessagingClient();
        client.setUserIdToFormatKey("userId");
        try {
            client.sendSync(settings, requestMessage);
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(MessagingException.class)));
            assertThat(e.getMessage(), is("HOGE is unsupported HTTP method."));
        } finally {
            SystemRepository.clear();
        }
    }

    /**
     * メッセージID送信のテスト
     */
    @Test
    public void testMessageId() {
        //ユニットテスト用フォーマット定義ファイル準備
        prepareFormatFileMessageId();

        SyncMessage requestMessage = null;
        SyncMessage reciveMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;

        //GET(メッセージ定義ファイルでメッセージID生成クラスの指定を行っている)
        requestMessage = new SyncMessage("RM21AB0600");
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>();
                        ArrayList<String> arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        treeMap.put("Content-Type", arrayList);
                        httpResult.setHeaderInfo(treeMap);
                        httpResult.setResponseCode(201);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        reciveMessage = client.sendSync(settings, requestMessage);
        
        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0600"));
        assertThat(client.getLastBodyText(), is(""));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHeaderInfo().get("X-Message-Id").size(), is(1));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.GET));
        Map<String, Object> headerRecord = reciveMessage.getHeaderRecord();
        assertThat((String)(headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)), is("201"));
        assertThat((String)(headerRecord.get("Content-Type")), is("application/json; charset=UTF-8"));
        Map<String, Object> dataRecord = reciveMessage.getDataRecord();
        assertThat((String)(dataRecord.get("messageCode")), is("100"));
        assertThat((String)(dataRecord.get("message")), is("OK"));
    }

    /**
     * ヘッダにBigDecimal型の値を設定した場合に、指数表記にならないことを確認する。
     */
    @Test
    public void testHeader_bigDecimal() {
        prepareFormatFileMessageId();

        SyncMessage requestMessage = new SyncMessage("RM21AB0600");
        requestMessage.getHeaderRecord().put("decimal", new BigDecimal("0.0000000001"));
        StubHTTPMessagingClient client = new StubHTTPMessagingClient();

        SyncMessage receiveMessage = client.sendSync(new MessageSenderSettings(requestMessage.getRequestId()), requestMessage);

        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0600"));
        assertThat(client.getLastBodyText(), is(""));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHeaderInfo().get("decimal").get(0), is("0.0000000001"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.GET));

        assertThat(receiveMessage.getHeaderRecord().get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE).toString(), is("200"));
    }

    private void prepareFormatFileMessageId(){
        File formatFile = null;
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0600_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }


    /***
     * 呼び出し側から「フレームワーク制御ヘッダー」のテスト。
     */
    @Test
    public void testFwHeader() {
        //ユニットテスト用フォーマット定義ファイル準備
        prepareFormatFileFwHeader();

        SyncMessage requestMessage = null;
        MessageSenderSettings settings = null;
        StubHTTPMessagingClient client= null;
        Map<String, Object> requestRecodeData = null;

        //POST
        //呼び出し側から「フレームワーク制御ヘッダー」のユーザIDの値をプログラム中で設定しなかった場合は、設定ファイルで指定した値が使用される。
        requestMessage = new SyncMessage("RM21AB0700");
        requestRecodeData = new TreeMap<String, Object>();
        requestRecodeData.put("requestId", "RM21AB0700");
        requestRecodeData.put("firstName", "太郎");
        requestRecodeData.put("lastName", "ナブラ");
        requestMessage.addDataRecord(requestRecodeData);
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>();
                        ArrayList<String> arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        treeMap.put("Content-Type", arrayList);
                        httpResult.setHeaderInfo(treeMap);
                        httpResult.setResponseCode(200);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        client.sendSync(settings, requestMessage);

        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0700"));
        assertThat(client.getLastBodyText(), is("{\"userId\":\"user01\",\"requestId\":\"RM21AB0700\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\"}"));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.POST));

        //POST
        //呼び出し側から「フレームワーク制御ヘッダー」のユーザIDの値をプログラム中で設定した場合は、プログラムで設定した値が使用される。
        requestMessage = new SyncMessage("RM21AB0700");
        requestRecodeData = new TreeMap<String, Object>();
        requestRecodeData.put("userId", "user02");
        requestRecodeData.put("requestId", "RM21AB0700");
        requestRecodeData.put("firstName", "太郎");
        requestRecodeData.put("lastName", "ナブラ");
        requestMessage.addDataRecord(requestRecodeData);
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>();
                        ArrayList<String> arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        treeMap.put("Content-Type", arrayList);
                        httpResult.setHeaderInfo(treeMap);
                        httpResult.setResponseCode(200);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        client.setUserIdToFormatKey("userId");
        client.sendSync(settings, requestMessage);

        assertThat(client.getLastUri(), is("http://localhost:8090/rm21ab0700"));
        assertThat(client.getLastBodyText(), is("{\"userId\":\"user02\",\"requestId\":\"RM21AB0700\",\"firstName\":\"太郎\",\"lastName\":\"ナブラ\"}"));
        assertThat(client.getLastCharset(), is("UTF-8"));
        assertThat(client.getLastHttpMethod(), is(HttpRequestMethodEnum.POST));

        //POST
        //呼び出し側から「フレームワーク制御ヘッダー」のユーザIDのキーを設定していない場合は、送信する電文に設定されない。
        requestMessage = new SyncMessage("RM21AB0700");
        requestRecodeData = new TreeMap<String, Object>();
        requestRecodeData.put("requestId", "RM21AB0700");
        requestRecodeData.put("firstName", "太郎");
        requestRecodeData.put("lastName", "ナブラ");
        requestMessage.addDataRecord(requestRecodeData);
        settings = new MessageSenderSettings(requestMessage.getRequestId());
        //応答を差し替えるためにスタブをインスタンス化する。
        client = new StubHTTPMessagingClient(){
            @Override
            protected HttpProtocolClient createHttpProtocolClient() {
                return new StubHttpProtocolClient(){
                    @Override
                    public HttpResult execute(HttpRequestMethodEnum httpMethod,
                            String url, Map<String, List<String>> headerInfo,
                            Map<String, String> urlParams,
                            HttpOutputStreamWriter writer,
                            HttpInputStreamReader reader) {
                        //ユニットテスト用の応答生成
                        HttpResult httpResult = new HttpResult();
                        TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>();
                        ArrayList<String> arrayList = new ArrayList<String>();
                        arrayList.add("application/json; charset=UTF-8");
                        treeMap.put("Content-Type", arrayList);
                        httpResult.setHeaderInfo(treeMap);
                        httpResult.setResponseCode(200);
                        String readObject = "{\"messageCode\":\"100\", \"message\":\"OK\"}";
                        httpResult.setReadObject(readObject);
                        return httpResult;
                    }
                };
            }
        };
        try {
            client.sendSync(settings, requestMessage);
            fail();
        } catch (Exception e) {
            //ユーザIDが存在しないため、送信する過程で例外が発生する。
            assertThat(e, is(instanceOf(MessagingException.class)));
        }
    }


    private void prepareFormatFileFwHeader(){
        File formatFile = null;

        // フォーマットファイル生成(リアルタイム通信　送信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0700_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 userId X
        2 requestId X
        3 firstName N
        4 lastName N
        *******/
        formatFile.deleteOnExit();
        
        // フォーマットファイル生成(リアルタイム通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0700_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }
    
    /***
     * ログアサート用のリストを作成します。
     * 出力されるログはデフォルトのフォーマッタで整形されていることを前提としています。
     * 
     * @param expectedRequestBody  要求電文
     * @param expectedDestination  送信先
     * @param expectedResponseBody 応答電文
     * @param expectedStatusCode   ステータスコード
     */
    private List<Map<String, String>> createExpectedLogMessages(
            String expectedRequestBody, String expectedDestination, String expectedResponseBody, int expectedStatusCode) {
        
        Map<String, String> sentLogMessage = new HashMap<String, String>();
        sentLogMessage.put("logLevel", "INFO");
        sentLogMessage.put("message1", "@@@@ HTTP SENT MESSAGE @@@@");
        sentLogMessage.put("message2", "destination    = [" + expectedDestination + "]");
        sentLogMessage.put("message3", "message_body   = [" + expectedRequestBody + "]");
        
        Map<String, String> receivedLogMessage = new HashMap<String, String>();
        receivedLogMessage.put("logLevel", "INFO");
        receivedLogMessage.put("message1", "@@@@ HTTP RECEIVED MESSAGE @@@@");
        receivedLogMessage.put("message2", "message_body   = [" + expectedResponseBody + "]");
        receivedLogMessage.put("message3", "STATUS_CODE=" + expectedStatusCode);
        
        List<Map<String, String>> expectedLogMessages = new ArrayList<Map<String,String>>();
        expectedLogMessages.add(sentLogMessage);
        expectedLogMessages.add(receivedLogMessage);
        
        return expectedLogMessages;
    }

}
