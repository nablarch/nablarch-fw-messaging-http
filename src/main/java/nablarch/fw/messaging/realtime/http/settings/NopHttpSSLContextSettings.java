package nablarch.fw.messaging.realtime.http.settings;

import javax.net.ssl.SSLContext;

import nablarch.fw.messaging.HttpSSLContextSettings;

/**
 * SSLContextを使用しない旨を表すクラス。<br/>
 * 
 * メッセージ送信定義ファイルの設定を「SSLContextなし」に上書きする際に使用する。
 * @author Masaya Seko
 */
public class NopHttpSSLContextSettings implements HttpSSLContextSettings {
    /**
     * SSLContextを取得する。
     * 本クラスは常にnullを返す。
     * @return SSLContext
     */
    public SSLContext getSSLContext() {
        return null;
    }

}
