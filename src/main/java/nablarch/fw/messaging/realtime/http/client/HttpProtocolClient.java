package nablarch.fw.messaging.realtime.http.client;

import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.realtime.http.dto.HttpResult;
import nablarch.fw.messaging.realtime.http.streamio.HttpInputStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.HttpOutputStreamWriter;

/**
 * Http接続用クライアントが実装すべきインターフェース。
 * 
 * @author Masaya Seko
 */
@Published(tag = "architect")
public interface HttpProtocolClient {
    /**
     * HTTP通信において使用可能なHTTPメソッド。
     */
    public enum HttpRequestMethodEnum {
        /** GETメソッド */
        GET("GET"),
        /** PUTメソッド */
        PUT("PUT"),
        /** POSTメソッド */
        POST("POST"),
        /** DELETEメソッド */
        DELETE("DELETE");

        /** メソッド*/
        private String method;

        /**
         * コンストラクタ。
         * @param method メソッド
         */
        private HttpRequestMethodEnum(String method) {
            this.method = method;
        }

        @Override
        public String toString() {
            return method;
        }
    }

    /**
     * プロキシ情報を設定します。
     * @param url プロキシURL
     * @param port ポート番号
     */
    void setProxyInfo(String url, int port);

    /**
     * SSLContextを設定する。
     * @param sslContext SSLContext
     */
    void setSslContext(SSLContext sslContext);

    /**
     * 接続タイムアウトを設定する。
     * @param connectTimeout 接続タイムアウト
     */
    void setConnectTimeout(int connectTimeout);

    /**
     * 読み取りタイムアウトを設定する。
     * @param httpReadTimeout 読み取りタイムアウト
     */
    void setReadTimeout(int httpReadTimeout);

    /**
     * コンテンツタイプを設定する。
     * @param contentType コンテンツタイプ
     */
    void setContentType(String contentType);

    /**
     * 受け入れるデータ種別を設定します。
     * @param accept 受け入れるデータ種別
     */
    void setAccept(String accept);

    /**
     * パラメータをエンコードする際に使用する文字コードを設定する。
     * @param queryStringEncoding 文字コード
     */
    void setQueryStringEncoding(String queryStringEncoding);
    
    /**
     * HTTP通信を行う。
     * @param httpMethod HTTPメソッド
     * @param url 接続先
     * @param headerInfo HttpHeadderに渡す情報
     * @param urlParams URLパラメータ
     * @param writer StreamWritter
     * @param reader OutputStreamReader
     * @return レスポンスコード
     */
    HttpResult execute(HttpRequestMethodEnum httpMethod, String url, Map<String, List<String>> headerInfo,
            Map<String, String> urlParams, HttpOutputStreamWriter writer, HttpInputStreamReader reader);
}
