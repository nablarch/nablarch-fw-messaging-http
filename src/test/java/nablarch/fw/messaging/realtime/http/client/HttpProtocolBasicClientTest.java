package nablarch.fw.messaging.realtime.http.client;

import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.realtime.http.client.HttpProtocolClient.HttpRequestMethodEnum;
import nablarch.fw.messaging.realtime.http.dto.HttpResult;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingException;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingTimeoutException;
import nablarch.fw.messaging.realtime.http.streamio.CharHttpStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.CharHttpStreamWritter;
import nablarch.fw.messaging.realtime.http.streamio.HttpInputStreamReader;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.servlet.HttpRequestWrapper;
import nablarch.test.support.tool.Hereis;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HttpProtocolBasicClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new HttpServer()
        .setPort(8766)
        .addHandler("/action/*.do", new RequestActions())
        .start();
    }

    /**
     * URLが不正なパターンで想定どおりの例外が返却されることを確認する
     */
    @Test
    public void testAbnormalURL() {
        HttpProtocolBasicClient httpProtocolBasicClient = new HttpProtocolBasicClient();
        Map<String, List<String>> headerInfo = new HashMap<String, List<String>>();

        // スキームが不正
        try {
            httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                    "hoge://localhost:8766/action/010.do", headerInfo, null, null, null);
            fail();
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(HttpMessagingException.class.getName()));
        }
        // スキームの後ろの:を多く
        try {
            httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                    "http:://localhost:8766/action/010.do", headerInfo, null, null, null);
            fail();
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(RuntimeException.class.getName()));
        }
        // スキームの後ろの/を多く
        try {
            httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                    "http:///localhost:8766/action/010.do", headerInfo, null, null, null);
            fail();
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(RuntimeException.class.getName()));
        }
        // 不正なホスト名
        try {
            httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                    "http://hoge:8766/action/010.do", headerInfo, null, null, null);
            fail();
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(HttpMessagingException.class.getName()));
        }
        // 不正なポート
        try {
            httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                    "http://localhost:999a/action/010.do", headerInfo, null, null, null);
            fail();
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(HttpMessagingException.class.getName()));
        }
        // ポートの前の:を多く
        try {
            httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                    "http://localhost::8766/action/010.do", headerInfo, null, null, null);
            fail();
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(HttpMessagingException.class.getName()));
        }
    }
    
    /**
     * GET Methodを用いて通信を行う
     * @throws Exception
     */
    @Test
    public void testGetMethod() throws Exception {
        HttpProtocolBasicClient httpProtocolBasicClient = null;
        HttpResult httpResult = null;
        HttpInputStreamReader streamReader = null;
        Map<String, List<String>> headerInfo = null;
        Map<String, String> urlParams = null;

        //readerなし、Body読み取りなし、URLパラメータなし
        streamReader = null;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.GET,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, null, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストGET応答"));
        
        //URLパラメータなし(オブジェクトは設定)
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.GET,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, null, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストGET応答"));

        //URLパラメータあり
        //追加のヘッダ情報有り
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        urlParams.put("a", "テストGET要求1");
        urlParams.put("b", "テストGET要求2");
        headerInfo = new HashMap<String, List<String>>();
        List<String> headerParamList = new ArrayList<String>();
        headerParamList.add("testval");
        headerInfo.put("x-Test", headerParamList);
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpProtocolBasicClient.setAccept("text/plain");
        httpProtocolBasicClient.setContentType("application/json");
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.GET,
                "http://localhost:8766/action/020.do", headerInfo, urlParams, null, streamReader);

        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストGET応答"));
        
        //Proxy有り
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpProtocolBasicClient.setProxyInfo("localhost", 8766);
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.GET,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, null, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストGET応答"));

        //ステータスコード4xxを受信
        streamReader = new CharHttpStreamReader();
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.GET,
                "http://localhost:8766/action/100.do", headerInfo, urlParams, null, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(400));
        assertThat((String)httpResult.getReadObject(), is("テストGET応答400"));

        //タイムアウトが発生
        streamReader = null;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpProtocolBasicClient.setReadTimeout(1);
        httpProtocolBasicClient.setConnectTimeout(1);
        try{
            httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.GET,
                    "http://localhost:8766/action/110.do", headerInfo, urlParams, null, streamReader);
            fail();
        }catch(HttpMessagingTimeoutException e){
            assertThat(e.getMessage(), is("Time-out occurs. URL=[http://localhost:8766/action/110.do]."));
        }
    }


    /**
     * POST Methodを用いて通信を行う
     * @throws Exception
     */
    @Test
    public void testPostMethod() throws Exception {
        HttpProtocolBasicClient httpProtocolBasicClient = null;
        HttpResult httpResult = null;
        CharHttpStreamWritter streamWriter = null;
        HttpInputStreamReader streamReader = null;
        Map<String, List<String>> headerInfo = null;
        Map<String, String> urlParams = null;

        //readerなし、Bodyの書き込みなし、URLパラメータなし
        streamReader = null;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("UTF-8");
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/000.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストPOST応答"));
        
        //URLパラメータなし
        streamReader = new CharHttpStreamReader();
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append("テストPOST要求");
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストPOST応答"));
        
        //Bodyの読み取りあり、URLパラメータなし(オブジェクトは設定)
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append("テストPOST要求");
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストPOST応答"));

        //record-separatorあり固定長の単一レコード
        String requestBody = StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40) + "\n";
        String expectedResponseBody = "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47) + "\n";
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("MS932");
        streamWriter.append(requestBody);
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/201.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is(expectedResponseBody));

        //record-separatorあり固定長の複数レコード
        requestBody = StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40) + "\n"
                    + StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40) + "\n"
        //サロゲートペア対応
                    + StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("🙀🙀🙀", 10, '　') + StringUtil.rpad("🙀🙀🙀", 10, '　') + StringUtil.repeat(' ', 40) + "\n";
        expectedResponseBody = "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47) + "\n"
                             + "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47) + "\n";
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append(requestBody);
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/202.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is(expectedResponseBody));
        
        //record-separatorなし固定長の単一レコード
        requestBody = StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40);
        expectedResponseBody = "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47);
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("MS932");
        streamWriter.append(requestBody);
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/203.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is(expectedResponseBody));

        //record-separatorなし固定長の複数レコード
        requestBody = StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40)
                    + StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40);
        expectedResponseBody = "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47)
                             + "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47);
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("MS932");
        streamWriter.append(requestBody);
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/204.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is(expectedResponseBody));
        
        //XML
        requestBody = Hereis.string();
        /*********************************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <request>
          <_nbctlhdr>
            <userId>unitTest</userId>
            <resendFlag>0</resendFlag>
          </_nbctlhdr>
          <user>
            <id>nablarch</id>
            <name>ナブラーク</name>
          </user>
        </request>
        **********************************************************/
        
        expectedResponseBody = Hereis.string();
        /*********************************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <response>
          <_nbctlhdr>
            <statusCode>200</statusCode>
          </_nbctlhdr>
          <result>
            <msg>OK</msg>
          </result>
        </response>
        **********************************************************/
        
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append(requestBody);
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/205.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is(expectedResponseBody));
        
        //JSON
        requestBody = Hereis.string();
        /*********************************************************
        {"_nbctlhdr":
          {"userId":"unitTest"
          ,"resendFlag":"0"
          }
        ,"user":
          {"id":"nablarch"
          ,"name":"ナブラーク"
          }
        }
        **********************************************************/
        
        expectedResponseBody = Hereis.string();
        /*********************************************************
        {"_nbctlhdr":
          {"statusCode":"200"
          }
        ,"result":
          {"msg":"OK"
          }
        }
        **********************************************************/
        
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append(requestBody);
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/206.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is(expectedResponseBody));
        
        //URLパラメータあり
        //追加のヘッダ情報有り
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        urlParams.put("a", "テストPOST要求1");
        urlParams.put("b", "テストPOST要求2");
        headerInfo = new HashMap<String, List<String>>();
        List<String> headerParamList = new ArrayList<String>();
        headerParamList.add("testval");
        headerInfo.put("x-Test", headerParamList);
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append("テストPOST要求");
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/020.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストPOST応答"));

        //ステータスコード4xxを受信
        streamReader = new CharHttpStreamReader();
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                "http://localhost:8766/action/100.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(400));
        assertThat((String)httpResult.getReadObject(), is("テストPOST応答400"));

        //タイムアウトが発生
        streamReader = null;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpProtocolBasicClient.setReadTimeout(1);
        httpProtocolBasicClient.setConnectTimeout(1);
        try{
            httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.POST,
                    "http://localhost:8766/action/110.do", headerInfo, urlParams, streamWriter, streamReader);
            fail();
        }catch(HttpMessagingTimeoutException e){
            assertThat(e.getMessage(), is("Time-out occurs. URL=[http://localhost:8766/action/110.do]."));
        }
    }

    /**
     * PUT Methodを用いて通信を行う
     * @throws Exception
     */
    @Test
    public void testPutMethod() throws Exception {
        HttpProtocolBasicClient httpProtocolBasicClient = null;
        HttpResult httpResult = null;
        CharHttpStreamWritter streamWriter = null;
        HttpInputStreamReader streamReader = null;
        Map<String, List<String>> headerInfo = null;
        Map<String, String> urlParams = null;

        
        //readerなし、Body読み取りなし、URLパラメータなし
        streamReader = null;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append("テストPUT要求");
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.PUT,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストPUT応答"));
        
        //URLパラメータなし(オブジェクトは設定)
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append("テストPUT要求");
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.PUT,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストPUT応答"));

        //URLパラメータあり
        //追加のヘッダ情報有り
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        urlParams.put("a", "テストPUT要求1");
        urlParams.put("b", "テストPUT要求2");
        headerInfo = new HashMap<String, List<String>>();
        List<String> headerParamList = new ArrayList<String>();
        headerParamList.add("testval");
        headerInfo.put("x-Test", headerParamList);
        streamWriter = new CharHttpStreamWritter("UTF-8");
        streamWriter.append("テストPUT要求");
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.PUT,
                "http://localhost:8766/action/020.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストPUT応答"));

        //ステータスコード4xxを受信
        streamReader = new CharHttpStreamReader();;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.PUT,
                "http://localhost:8766/action/100.do", headerInfo, urlParams, streamWriter, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(400));
        assertThat((String)httpResult.getReadObject(), is("テストPUT応答400"));

        //タイムアウトが発生
        streamReader = null;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpProtocolBasicClient.setReadTimeout(1);
        httpProtocolBasicClient.setConnectTimeout(1);
        try{
            httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.PUT,
                    "http://localhost:8766/action/110.do", headerInfo, urlParams, streamWriter, streamReader);
            fail();
        }catch(HttpMessagingTimeoutException e){
            assertThat(e.getMessage(), is("Time-out occurs. URL=[http://localhost:8766/action/110.do]."));
        }
    }
    

    /**
     * Delete Methodを用いて通信を行う
     * @throws Exception
     */
    @Test
    public void testDeleteMethod() throws Exception {
        HttpProtocolBasicClient httpProtocolBasicClient = null;
        HttpResult httpResult = null;
        HttpInputStreamReader streamReader = null;
        Map<String, List<String>> headerInfo = null;
        Map<String, String> urlParams = null;

        //readerなし、Bodyの読み取りなし、URLパラメータなし
        streamReader = null;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.DELETE,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, null, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストDELETE応答"));
        
        //Bodyの読み取りあり、URLパラメータなし(オブジェクトは設定)
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.DELETE,
                "http://localhost:8766/action/010.do", headerInfo, urlParams, null, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストDELETE応答"));

        //Bodyの読み取りあり、URLパラメータあり
        //追加のヘッダ情報有り
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        urlParams.put("a", "テストDELETE要求1");
        urlParams.put("b", "テストDELETE要求2");
        headerInfo = new HashMap<String, List<String>>();
        List<String> headerParamList = new ArrayList<String>();
        headerParamList.add("testval");
        headerInfo.put("x-Test", headerParamList);
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.DELETE,
                "http://localhost:8766/action/020.do", headerInfo, urlParams, null, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(200));
        assertThat((String)httpResult.getReadObject(), is("テストDELETE応答"));

        //ステータスコード4xxを受信
        streamReader = new CharHttpStreamReader();;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.DELETE,
                "http://localhost:8766/action/100.do", headerInfo, urlParams, null, streamReader);
        assertNotNull(httpResult.getHeaderInfo().get(null).get(0));
        assertThat(httpResult.getResponseCode(), is(400));
        assertThat((String)httpResult.getReadObject(), is("テストDELETE応答400"));

        //タイムアウトが発生
        streamReader = null;
        urlParams = null;
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();
        httpProtocolBasicClient.setReadTimeout(1);
        httpProtocolBasicClient.setConnectTimeout(1);
        try{
            httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.DELETE,
                    "http://localhost:8766/action/110.do", headerInfo, urlParams, null, streamReader);
            fail();
        }catch(HttpMessagingTimeoutException e){
            assertThat(e.getMessage(), is("Time-out occurs. URL=[http://localhost:8766/action/110.do]."));
        }
    }
    
    /**
     * 不正なエンコーディングでURLパラメータをエンコーディングした場合。
     * GET Methodを用いて通信を行う
     * @throws Exception
     */
    @Test
    public void testUrlParamInvalidEncoding() throws Exception {
        HttpProtocolBasicClient httpProtocolBasicClient = null;
        HttpResult httpResult = null;
        HttpInputStreamReader streamReader = null;
        Map<String, List<String>> headerInfo = null;
        Map<String, String> urlParams = null;
        //URLパラメータあり
        //追加のヘッダ情報有り
        streamReader = new CharHttpStreamReader();
        urlParams = new HashMap<String, String>();
        urlParams.put("a", "テストGET要求1");
        urlParams.put("b", "テストGET要求2");
        headerInfo = new HashMap<String, List<String>>();
        httpProtocolBasicClient = new HttpProtocolBasicClient();

        //ありえないエンコーディング
        httpProtocolBasicClient.setQueryStringEncoding("hogehoge");
        
        httpProtocolBasicClient.setAccept("text/plain");
        httpProtocolBasicClient.setContentType("application/json");
        try{
            httpResult = httpProtocolBasicClient.execute(HttpRequestMethodEnum.GET,
                    "http://localhost:8766/action/020.do", headerInfo, urlParams, null, streamReader);
            fail();
        }catch(MessagingException e){
            assertThat(e.getMessage(), is("java.io.UnsupportedEncodingException: hogehoge"));
        }
    }

    /**
     * SSL用コネクションが生成できること
     * @throws Exception
     */
    @Test
    public void testCreateSSLHttpConnection() throws Exception {
        SSLTestHttpProtocolBasicClient client = null;
        HttpURLConnection connection = null;
        
        client = new SSLTestHttpProtocolBasicClient();
        connection = client.createHttpConnectionWrapper("https://localhost:8766/action/010.do", HttpRequestMethodEnum.GET, new HashMap<String, List<String>>());
        assertThat(connection, instanceOf(HttpsURLConnection.class));

        client = new SSLTestHttpProtocolBasicClient();
        client.setSslContext(SSLContext.getDefault());
        connection = client.createHttpConnectionWrapper("https://localhost:8766/action/010.do", HttpRequestMethodEnum.GET, new HashMap<String, List<String>>());
        assertThat(connection, instanceOf(HttpsURLConnection.class));

    }
    
    public static class SSLTestHttpProtocolBasicClient extends HttpProtocolBasicClient{
        public HttpURLConnection createHttpConnectionWrapper(String targetUrl,
                HttpRequestMethodEnum method, Map<String, List<String>> headerInfo) throws IOException {
            return createHttpConnection(targetUrl, method, headerInfo);
            
        }
    }
    
    public static class RequestActions {
        /**
         * 業務アクション010(GET)
         */
        public HttpResponse get010do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "テストGET応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション020(GET)
         */
        public HttpResponse get020do(HttpRequest req, ExecutionContext ctx) throws Exception {
            assertThat(req.getParam("a")[0], is("テストGET要求1"));
            assertThat(req.getParam("b")[0], is("テストGET要求2"));
            assertThat(req.getHeaderMap().get("x-Test"), is("testval"));
            assertThat(req.getHeaderMap().get("content-type"), is("application/json"));
            assertThat(req.getHeaderMap().get("accept"), is("text/plain"));
            String responseBody = "テストGET応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション100(GET)
         */
        public HttpResponse get100do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "テストGET応答400";
            return new HttpResponse(400)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション110(GET)
         */
        public HttpResponse get110do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "";
            Thread.sleep(1000);
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション000(POST)
         */
        public HttpResponse post000do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            
            assertEquals("", baos.toString("UTF-8"));
            
            String responseBody = "テストPOST応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション010(POST)
         */
        public HttpResponse post010do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            
            assertEquals("テストPOST要求", baos.toString("UTF-8"));
            
            String responseBody = "テストPOST応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション020(POST)
         */
        public HttpResponse post020do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            
            assertThat(req.getParam("a")[0], is("テストPOST要求1"));
            assertThat(req.getParam("b")[0], is("テストPOST要求2"));
            assertThat(req.getHeaderMap().get("x-Test"), is("testval"));
            assertEquals("テストPOST要求", baos.toString("UTF-8"));
            
            String responseBody = "テストPOST応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション100(POST)
         */
        public HttpResponse post100do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "テストPOST応答400";
            return new HttpResponse(400)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション110(POST)
         */
        public HttpResponse post110do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "";
            Thread.sleep(1000);
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション201(POST)
         * record-separatorあり固定長の単一レコード
         */
        public HttpResponse post201do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            String requestBody = baos.toString("MS932");
            String expectedRequestBody = StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40) + "\n";
            assertThat(requestBody, is(expectedRequestBody));
            
            String responseBody = "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47) + "\n";
            
            return new HttpResponse(200)
                  .setContentType("text/plain; charset=MS932")
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("MS932")));
        }

        /**
         * 業務アクション202(POST)
         * record-separatorあり固定長の複数レコード
         */
        public HttpResponse post202do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            String requestBody = baos.toString("UTF-8");
            String expectedRequestBody = StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40) + "\n"
                                       + StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40) + "\n"
                                       + StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("🙀🙀🙀", 10, '　') + StringUtil.rpad("🙀🙀🙀", 10, '　') + StringUtil.repeat(' ', 40) + "\n";
            assertThat(requestBody, is(expectedRequestBody));
            
            String responseBody = "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47) + "\n"
                                + "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47) + "\n";
            
            return new HttpResponse(200)
                  .setContentType("text/plain; charset=MS932")
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("MS932")));
        }

        /**
         * 業務アクション203(POST)
         * record-separatorなし固定長の単一レコード
         */
        public HttpResponse post203do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            String requestBody = baos.toString("MS932");
            String expectedRequestBody = StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40);
            assertThat(requestBody, is(expectedRequestBody));
            
            String responseBody = "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47);
            
            return new HttpResponse(200)
                  .setContentType("text/plain; charset=MS932")
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("MS932")));
        }

        /**
         * 業務アクション204(POST)
         * record-separatorなし固定長の複数レコード
         */
        public HttpResponse post204do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            String requestBody = baos.toString("MS932");
            String expectedRequestBody = StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40)
                                       + StringUtil.rpad("RM21AB0203", 20, ' ') + StringUtil.rpad("太郎", 10, '　') + StringUtil.rpad("ナブラ", 10, '　') + StringUtil.repeat(' ', 40);
            assertThat(requestBody, is(expectedRequestBody));
            
            String responseBody = "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47)
                                + "100" + StringUtil.rpad("OK", 50, ' ') + StringUtil.repeat(' ', 47);
            
            return new HttpResponse(200)
                  .setContentType("text/plain; charset=MS932")
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("MS932")));
        }

        /**
         * 業務アクション205(POST)
         * XML
         */
        public HttpResponse post205do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            String requestBody = baos.toString("UTF-8");
            String expectedRequestBody = Hereis.string();
            /*********************************************************
            <?xml version="1.0" encoding="UTF-8"?>
            <request>
              <_nbctlhdr>
                <userId>unitTest</userId>
                <resendFlag>0</resendFlag>
              </_nbctlhdr>
              <user>
                <id>nablarch</id>
                <name>ナブラーク</name>
              </user>
            </request>
            **********************************************************/
            
            assertThat(requestBody, is(expectedRequestBody));
            
            String responseBody = Hereis.string();
            /*********************************************************
            <?xml version="1.0" encoding="UTF-8"?>
            <response>
              <_nbctlhdr>
                <statusCode>200</statusCode>
              </_nbctlhdr>
              <result>
                <msg>OK</msg>
              </result>
            </response>
            **********************************************************/
            
            
            return new HttpResponse(200)
            .setContentType("application/xml; charset=UTF-8")
            .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }
        
        /**
         * 業務アクション206(POST)
         * JSON
         */
        public HttpResponse post206do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            String requestBody = baos.toString("UTF-8");
            String expectedRequestBody = Hereis.string();
            /*********************************************************
            {"_nbctlhdr":
              {"userId":"unitTest"
              ,"resendFlag":"0"
              }
            ,"user":
              {"id":"nablarch"
              ,"name":"ナブラーク"
              }
            }
            **********************************************************/
            
            assertThat(requestBody, is(expectedRequestBody));
            
            String responseBody = Hereis.string();
            /*********************************************************
            {"_nbctlhdr":
              {"statusCode":"200"
              }
            ,"result":
              {"msg":"OK"
              }
            }
            **********************************************************/
            
            
            return new HttpResponse(200)
            .setContentType("application/xml; charset=UTF-8")
            .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }
        

        /**
         * 業務アクション000(PUT)
         */
        public HttpResponse put000do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            
            String responseBody = "テストPUT応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション010(PUT)
         */
        public HttpResponse put010do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            
            assertEquals("テストPUT要求", baos.toString("UTF-8"));
            
            String responseBody = "テストPUT応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション020(PUT)
         */
        public HttpResponse put020do(HttpRequest req, ExecutionContext ctx) throws Exception {
            InputStream is = ((HttpRequestWrapper)req).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            
            assertThat(req.getParam("a")[0], is("テストPUT要求1"));
            assertThat(req.getParam("b")[0], is("テストPUT要求2"));
            assertThat(req.getHeaderMap().get("x-Test"), is("testval"));
            assertEquals("テストPUT要求", baos.toString("UTF-8"));
            
            String responseBody = "テストPUT応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション100(PUT)
         */
        public HttpResponse put100do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "テストPUT応答400";
            return new HttpResponse(400)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション110(PUT)
         */
        public HttpResponse put110do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "";
            Thread.sleep(1000);
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション010(DELETE)
         */
        public HttpResponse delete010do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "テストDELETE応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション020(DELETE)
         */
        public HttpResponse delete020do(HttpRequest req, ExecutionContext ctx) throws Exception {
            assertThat(req.getParam("a")[0], is("テストDELETE要求1"));
            assertThat(req.getParam("b")[0], is("テストDELETE要求2"));
            assertThat(req.getHeaderMap().get("x-Test"), is("testval"));
            String responseBody = "テストDELETE応答";
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション100(DELETE)
         */
        public HttpResponse delete100do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "テストDELETE応答400";
            return new HttpResponse(400)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }

        /**
         * 業務アクション110(DELETE)
         */
        public HttpResponse delete110do(HttpRequest req, ExecutionContext ctx) throws Exception {
            String responseBody = "";
            Thread.sleep(1000);
            return new HttpResponse(200)
                  .setBodyStream(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        }
    }

}
