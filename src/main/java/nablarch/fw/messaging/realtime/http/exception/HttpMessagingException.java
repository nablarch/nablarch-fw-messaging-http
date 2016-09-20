package nablarch.fw.messaging.realtime.http.exception;

import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.MessagingException;

/**
 * HTTP通信に関する例外クラス。
 * @author Masaya Seko
 */
@Published
public class HttpMessagingException extends MessagingException {

    /** 例外発生時の接続先*/
    private String targetUrl;

    /** 例外発生時のレスポンスコード*/
    private Integer statusCode;

    /**
     * 詳細メッセージに{@code null}を使用して、{@code HttpMessagingException}を構築する。
     */
    public HttpMessagingException() {
        super();
    }
    

    /**
     * 指定された詳細メッセージおよび起因となる例外を使用して、{@code HttpMessagingException}を構築する。
     * @param message 詳細メッセージ
     * @param cause 起因となる例外
     */
    public HttpMessagingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 指定された詳細メッセージを使用して、{@code HttpMessagingException}を構築する。
     * @param message 詳細メッセージ
     */
    public HttpMessagingException(String message) {
        super(message);
    }

    /**
     * 指定された起因となる例外を使用して、{@code HttpMessagingException}を構築する。
     * @param cause 起因となる例外
     */
    public HttpMessagingException(Throwable cause) {
        super(cause);
    }

    /**
     * 指定された起因となる例外を使用し、例外発生時の接続先及びHTTPステータスコードを指定して
     * {@code HttpMessagingException}を構築する。
     *
     * @param targetUrl 例外発生時の接続先
     * @param statusCode HTTPステータスコード
     * @param cause 起因となる例外
     */
    public HttpMessagingException(String targetUrl, Integer statusCode, Throwable cause) {
        super(cause);
        this.targetUrl = targetUrl;
        this.statusCode = statusCode;
    }

    /**
     * 指定された詳細メッセージおよび起因となる例外を使用し、
     * 例外発生時の接続先及びHTTPステータスコードを指定して{@code HttpMessagingException}を構築する。
     *
     * @param message エラーメッセージ
     * @param targetUrl 例外発生時の接続先
     * @param statusCode HTTPステータスコード
     * @param cause 起因となる例外
     */
    public HttpMessagingException(String message, String targetUrl, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.targetUrl = targetUrl;
        this.statusCode = statusCode;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * メッセージには以下の内容が含まれる。
     * ※インスタンス生成時に設定した項目のみ出力される。
     * <ul>
     *     <li>エラーメッセージ</li>
     *     <li>例外発生時の接続先</li>
     *     <li>HTTPステータスコード</li>
     * </ul>
     *
     * @return メッセージ
     */
    @Override
    public String getMessage() {

        String baseMsg = super.getMessage();
        boolean hasProp = StringUtil.hasValue(targetUrl) || statusCode != null;

        if (baseMsg == null && !hasProp) {
            return baseMsg;
        }

        if (baseMsg == null) {
            baseMsg = "";
        }

        StringBuilder msg = new StringBuilder(baseMsg);

        if (StringUtil.hasValue(targetUrl)) {
            msg.append(" URL=[").append(targetUrl).append("].");
        }

        if (statusCode != null) {
            msg.append(" status code=[").append(statusCode.toString()).append("].");
        } 

        return msg.toString();
    }

    /**
     * 例外発生時の接続先を取得する。
     * @return 例外発生時の接続先
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * 例外発生時の接続先を設定する。
     * @param targetUrl 接続先URL
     */
    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    /**
     * 例外発生時のHTTPステータスコードを取得する。
     * @return 例外発生時のHTTPステータスコード
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * 例外発生時のHTTPステータスコードを設定する。
     * @param statusCode HTTPステータスコード
     */
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

}
