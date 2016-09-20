package nablarch.fw.messaging.realtime.http.exception;

import java.util.List;
import java.util.Map;

import nablarch.core.util.annotation.Published;

/**
 * HTTP通信時、送受信したデータのフォーマット変換に失敗した際に送出される例外クラス。
 * @author Masaya Seko
 */
@Published
public class HttpMessagingInvalidDataFormatException extends HttpMessagingException {
    /**フォーマット変換しようとした送信データ。*/
    private Map<String, ?> requestData = null;

    /**受信したヘッダ情報。*/
    private Map<String, List<String>> headerInfo = null;

    /**フォーマット変換しようとした受信データ。*/
    private String receiveData = null;

    /**
     * 詳細メッセージに{@code null}を使用して、{@code HttpMessagingInvalidDataFormatException}を生成する。
     */
    public HttpMessagingInvalidDataFormatException() {
        super();
    }
    
    /**
     * 送信データのフォーマット変換に失敗したことを表す{@code HttpMessagingInvalidDataFormatException}を生成する。
     *
     * @param message エラーメッセージ
     * @param targetUrl 接続先
     * @param requestData フォーマット変換しようとした送信データ
     * @param cause 起因となる例外
     */
    public HttpMessagingInvalidDataFormatException(String message,
            String targetUrl, Map<String, ?> requestData, Throwable cause) {
        super(message, targetUrl, null, cause);
        this.requestData = requestData;
    }

    /**
     * 受信データのフォーマット変換に失敗したことを表す{@code HttpMessagingInvalidDataFormatException}を生成する。
     *
     * @param message エラーメッセージ
     * @param targetUrl 接続先
     * @param statusCode 受信したHTTPステータスコード
     * @param headerInfo 受信したヘッダ情報
     * @param receiveData フォーマット変換しようとした受信データ
     * @param cause 起因となる例外
     */
    public HttpMessagingInvalidDataFormatException(String message,
            String targetUrl, Integer statusCode, Map<String, List<String>> headerInfo, String receiveData, Throwable cause) {
        super(message, targetUrl, statusCode, cause);
        this.receiveData = receiveData; 
    }

    /**
     * 受信したヘッダ情報を取得する。
     * <p/>
     * 本例外をキャッチ後、フォーマット変換時のヘッダ情報が必要であれば、本メソッドを用いて取り出す。
     *
     * @return ヘッダ情報
     */
    public Map<String, List<String>> getHeaderInfo() {
        return headerInfo;
    }

    /**
     * 受信したヘッダ情報を設定する。
     * @param headerInfo ヘッダ情報
     */
    public void setHeaderInfo(Map<String, List<String>> headerInfo) {
        this.headerInfo = headerInfo;
    }

    /**
     * フォーマット変換しようとした受信データを取得する。
     * <p/>
     * 本例外をキャッチ後、変換前の受信データが必要であれば、本メソッドを用いて取り出す。
     *
     * @return フォーマット変換しようとした受信データ。
     *     この例外オブジェクトが送信データのフォーマット変換失敗を表している場合は{@code null}
     */
    public String getReceiveData() {
        return receiveData;
    }

    /**
     * フォーマット変換しようとした受信データを設定する。
     * @param receiveData フォーマット変換しようとした受信データ
     */
    public void setReceiveData(String receiveData) {
        this.receiveData = receiveData;
    }

    /**
     * フォーマット変換しようとした送信データを取得する。
     * @return フォーマット変換しようとした送信データ。
     *     この例外オブジェクトが受信データのフォーマット変換失敗を表している場合は{@code null}
     */
    public Map<String, ?> getRequestData() {
        return requestData;
    }

    /**
     * フォーマット変換しようとした送信データを設定する。
     * @param requestData フォーマット変換しようとした送信データ。
     */
    public void setRequestData(Map<String, ?> requestData) {
        this.requestData = requestData;
    }
}
