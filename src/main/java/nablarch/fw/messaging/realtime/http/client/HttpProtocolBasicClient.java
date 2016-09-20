package nablarch.fw.messaging.realtime.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import nablarch.core.util.FileUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.realtime.http.dto.HttpResult;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingException;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingTimeoutException;
import nablarch.fw.messaging.realtime.http.streamio.CharHttpStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.HttpInputStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.HttpOutputStreamWriter;

/**
 * Http接続用クライアント。
 * 
 * @author TIS
 */
@Published(tag = "architect")
public class HttpProtocolBasicClient implements HttpProtocolClient {
    /** HTTPSで通信する際に使用する情報 */
    private SSLContext sslContext;
    /** 接続タイムアウト */
    private int connectTimeout;
    /** 読みとりタイムアウト */
    private int readTimeout;
    /** プロキシURL */
    private String proxyUrl;
    /** プロキシポート */
    private int proxyPort;
    /** コンテンツタイプ */
    private String contentType;
    /** 受け入れるデータ種別 */
    private String accept;
    /** クエリストリングをエンコードする際に使用するエンコーディング*/
    private String queryStringEncoding = "UTF-8";

    /**
     * コンストラクタ
     */
    public HttpProtocolBasicClient() {
        super();
    }

    /**
     * プロキシ情報を設定する。
     * @param url プロキシURL
     * @param port ポート番号
     */
    public void setProxyInfo(String url, int port) {
        this.proxyUrl = url;
        this.proxyPort = port;
    }

    /**
     * 接続タイムアウトを設定する。
     * @param connectTimeout 接続タイムアウト
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * 読み取りタイムアウトを設定する。
     * @param readTimeout 読み取りタイムアウト
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * コンテンツタイプを設定する。
     * @param contentType コンテンツタイプ
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 受け入れるデータ種別を設定する。
     * @param accept 受け入れるデータ種別
     */
    public void setAccept(String accept) {
        this.accept = accept;
    }

    /**
     * SSLContextを設定する。
     * @param sslContext SSLContext
     */
    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * パラメータをエンコードする際に使用する文字コードを設定する。
     * @param queryStringEncoding 文字コード
     */
    public void setQueryStringEncoding(String queryStringEncoding) {
        this.queryStringEncoding = queryStringEncoding;
    }

    /**
     * HTTP通信を行う。
     * @param httpMethod HTTPメソッド
     * @param url 接続先
     * @param headerInfo HttpHeadderに渡す情報
     * @param urlParams URLパラメータ。送信するパラメータがない場合は、null可。
     * @param writer StreamWritter。送信するBody部が存在しない場合は、null可。
     * @param reader 応答の読み込みに使用するreader
     * @return レスポンスコード
     * @throws HttpMessagingException 何らかの理由(接続タイムアウト、ソケットの予期せぬclose等)で通信が失敗した場合に送出される。
     */
    public HttpResult execute(HttpRequestMethodEnum httpMethod, String url, Map<String, List<String>> headerInfo,
            Map<String, String> urlParams, HttpOutputStreamWriter writer, HttpInputStreamReader reader)
                    throws HttpMessagingException {

        //通信結果
        HttpResult result = new HttpResult();
        // 接続先URL
        String targetUrl = url;
        //ヘッダ情報
        Map<String, List<String>> headeInfo = null;
        //ステータスコード
        Integer responseCode = null;
        //レスポンス本体
        Object responseObject = null;
        // パラメータが設定されていたら、URLにパラメータを追加
        if (urlParams != null && !urlParams.isEmpty()) {
            targetUrl = targetUrl + "?" + paramStringBuilder(urlParams);
        }
        
        if (reader == null) {
            //リーダーが指定されていない場合は、新たにリーダーを生成する。
            reader = new CharHttpStreamReader();
        }
        
        HttpURLConnection con = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            //書き込みHTTPステータスコードとヘッダ情報の読み取り
            con = createHttpConnection(targetUrl, httpMethod, headerInfo);
            if (writer != null) {
                outputStream = con.getOutputStream();
                writer.writeStream(outputStream);
            }
            //以降、Streamへの書き込みはないのでcloseする。
            FileUtil.closeQuietly(outputStream);
            outputStream = null;

            //HTTPステータスコードとヘッダ情報の読み取り
            responseCode = con.getResponseCode();
            headeInfo = con.getHeaderFields();
            reader.setHeaderInfo(headeInfo);

            //body部の読み取り
            inputStream = con.getInputStream();
            responseObject = reader.readInputStream(inputStream);
        } catch (SocketTimeoutException e) {
            throw new HttpMessagingTimeoutException("Time-out occurs.", targetUrl, responseCode, e);
        } catch (IOException ie) {
            InputStream es = null;
            try {
                //エラー用ストリームからの読み取りを試す。
                //(何種類かのHTTPステータスコードについては、Body部の読み取り時にこのロジックに到達する)
                if (con != null) {
                    es = con.getErrorStream();
                }
                if (es != null) {
                    //エラー用ストリームから読み取れた場合は、処理を続行する。
                    responseObject = reader.readInputStream(es);
                } else {
                    //getErrorStream()からも読み取れない場合は、例外を送出する。
                    throw new HttpMessagingException(targetUrl, responseCode, ie);
                }
            } catch (SocketTimeoutException e) {
                throw new HttpMessagingTimeoutException("Time-out occurs.", targetUrl, responseCode, e);
            } catch (IOException e) {
                //getErrorStream()からも読み取れない場合は、例外を送出する。
                throw new HttpMessagingException(targetUrl, responseCode, e);
            } finally {
                FileUtil.closeQuietly(es);
            }
        } finally {
          //正常ケースでoutputStreamを閉じた場合、この行に到達した時点でnullが代入されているため、「2回stream閉じようとして例外が発生」ということはない
            FileUtil.closeQuietly(outputStream); 
            
            FileUtil.closeQuietly(inputStream);
            if (con != null) {
                con.disconnect();
            }
        }

        //通信結果設定
        result.setResponseCode(responseCode);
        result.setHeaderInfo(headeInfo);
        result.setReadObject(responseObject);
        return result;
    }

    /**
     * HttpURLConnectionを作成する。
     * 
     * @param targetUrl 接続先
     * @param method 接続メソッド
     * @param headerInfo HttpHeadderに渡す情報
     * @return HttpURLConnection
     * @throws IOException 接続例外
     */
    protected HttpURLConnection createHttpConnection(String targetUrl,
            HttpRequestMethodEnum method, Map<String, List<String>> headerInfo) throws IOException {

        URL url = new URL(targetUrl);

        HttpURLConnection con;
        if (proxyUrl != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl, proxyPort));
            con = (HttpURLConnection) url.openConnection(proxy);
        } else {
            con = (HttpURLConnection) url.openConnection();
        }

        if (con instanceof HttpsURLConnection) {
            if (sslContext != null) {
                ((HttpsURLConnection) con).setSSLSocketFactory(sslContext.getSocketFactory());
            }
        }
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);

        // Header情報を設定
        con.setRequestProperty("Content-type", contentType);
        con.setRequestProperty("Accept", accept);
        // 追加のHeader情報を設定
        for (Entry<String, List<String>> headerInfoEntrySet : headerInfo.entrySet()) {
            for (String value : headerInfoEntrySet.getValue()) {
                con.addRequestProperty(headerInfoEntrySet.getKey(), value);
            }
        }

        // GET以外はOutputする
        if (!method.equals(HttpRequestMethodEnum.GET)) {
            con.setDoOutput(true);
        }
        con.setRequestMethod(method.toString());

        return con;
    }

    /**
     * URLにパラメータを文字列として埋め込む場合の文字列を作成して返却する。
     * 
     * @param paramData 作成元パラメータ
     * @return 作成結果
     */
    protected String paramStringBuilder(Map<String, String> paramData) {
        StringBuilder sbParam = new StringBuilder();

        for (Entry<String, String> entry : paramData.entrySet()) {
            if (sbParam.length() != 0) {
                sbParam.append("&");
            }
            String value = null;
            try {
                value = URLEncoder.encode(entry.getValue(), queryStringEncoding);
            } catch (UnsupportedEncodingException e) {
                throw new MessagingException(e);
            }
            sbParam.append(String.format("%s=%s", entry.getKey(), value));
        }

        return sbParam.toString();
    }
}
