package nablarch.fw.messaging.realtime.http.exception;

import nablarch.core.util.annotation.Published;

/**
 * メッセージング機能で、コネクションの接続及びデータの読み込み要求がタイムアウトした場合に送出される例外。
 * @author Masaya Seko
 */
@Published
public class HttpMessagingTimeoutException extends HttpMessagingException {

    /**
     * 詳細メッセージに{@code null}を使用して、{@code HttpMessagingTimeoutException}を構築する。
     */
    public HttpMessagingTimeoutException() {
        super();
    }

    /**
     * 指定された詳細メッセージおよび起因となる例外を使用して、{@code HttpMessagingTimeoutException}を構築する。
     * @param message 詳細メッセージ
     * @param cause   起因となる例外
     */
    public HttpMessagingTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 指定された詳細メッセージおよび起因となる例外を使用し、例外発生時の接続先及びHTTPステータスコードを指定して
     * {@code HttpMessagingTimeoutException}を構築する。
     *
     * @param message 詳細メッセージ
     * @param targetUrl 接続先
     * @param statusCode HTTPステータスコード
     * @param cause 起因となる例外
     */
    public HttpMessagingTimeoutException(String message, String targetUrl, Integer statusCode, Throwable cause) {
        super(message, targetUrl, statusCode, cause);
    }
    
    /**
     * 指定された詳細メッセージを使用して、{@code HttpMessagingTimeoutException}を構築する。
     * @param message 詳細メッセージ
     */
    public HttpMessagingTimeoutException(String message) {
        super(message);
    }

    /**
     * 指定された起因となる例外を使用して、{@code HttpMessagingTimeoutException}を構築する。
     * @param cause   起因となる例外
     */
    public HttpMessagingTimeoutException(Throwable cause) {
        super(cause);
    }

}
