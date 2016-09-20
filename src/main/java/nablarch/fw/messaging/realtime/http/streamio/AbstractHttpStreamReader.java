package nablarch.fw.messaging.realtime.http.streamio;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nablarch.core.util.annotation.Published;

/**
 * HTTP通信時に使用するReaderクラス。
 * 
 * @author TIS
 */
@Published(tag = "architect")
public abstract class AbstractHttpStreamReader implements HttpInputStreamReader {
    /**
     * ヘッダ情報を設定する。
     * @param headerFields ヘッダ情報
     */
    public void setHeaderInfo(Map<String, List<String>> headerFields) {
        for (Entry<String, List<String>> entry : headerFields.entrySet()) {
            setParamsFromHeader(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * ヘッダー情報から取得した値をもとにStreamを読み込む際のパラメータを取得する。
     * @param key ヘッダーのKey
     * @param values Keyに設定されている値
     */
    protected abstract void setParamsFromHeader(String key, List<String> values);

}
