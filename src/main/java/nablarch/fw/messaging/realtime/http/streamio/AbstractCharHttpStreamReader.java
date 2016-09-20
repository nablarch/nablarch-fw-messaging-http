package nablarch.fw.messaging.realtime.http.streamio;

import java.util.List;

import nablarch.core.util.annotation.Published;

/**
 * HTTP通信時に使用するReaderクラス。
 * 
 * @author TIS
 */
@Published(tag = "architect")
public abstract class AbstractCharHttpStreamReader extends AbstractHttpStreamReader {

    /** エンコード*/
    private String encode = "ISO-8859-1";

    /**
     * コネクションのヘッダー情報を取得/解析する。
     * @param key ヘッダー情報Key
     * @param values 値
     */
    protected void setParamsFromHeader(String key, List<String> values) {
        if ("Content-Type".equals(key)) {
            setContentTypeFromHeader(values);
        }
    }

    /**
     * ContentTypeの設定を行う。
     * @param values コンテンツタイプ
     */
    protected void setContentTypeFromHeader(List<String> values) {
        for (String tmpVal : values) {
            String[] singleVal = tmpVal.split(";");

            for (String splitVal : singleVal) {
                String targetVal = splitVal.trim();
                if (targetVal.startsWith("charset=")) {
                    setEncode(targetVal.substring(targetVal.lastIndexOf("=") + 1, targetVal.length()));
                }
            }
        }
    }

    /**
     * エンコードを取得する。
     * @return エンコード
     */
    public String getEncode() {
        return encode;
    }

    /**
     * エンコードを設定する。
     * @param encode エンコード
     */
    public void setEncode(String encode) {
        this.encode = encode;
    }
}
