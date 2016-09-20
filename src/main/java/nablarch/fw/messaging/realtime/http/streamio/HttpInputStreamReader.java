package nablarch.fw.messaging.realtime.http.streamio;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import nablarch.core.util.annotation.Published;

/**
 * Streamを読み取り結果を返却させるためのIF
 * 
 * @author TIS
 */
@Published(tag = "architect")
public interface HttpInputStreamReader {

    /**
     * HttpHeader情報を設定する。
     * @param headerInfo HttpHeader情報
     */
    void setHeaderInfo(Map<String, List<String>> headerInfo);

    /**
     * InputStreamからデータを読み取り返却する。
     * @param os 読み取り対象
     * @return OutputStreamの読み取り結果
     * @throws IOException 読み取り例外
     */
    Object readInputStream(InputStream os) throws IOException;

}
