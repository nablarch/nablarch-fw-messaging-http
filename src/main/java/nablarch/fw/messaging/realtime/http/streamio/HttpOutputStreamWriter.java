package nablarch.fw.messaging.realtime.http.streamio;

import java.io.IOException;
import java.io.OutputStream;

import nablarch.core.util.annotation.Published;

/**
 * Streamに書き出しを行わせるためのIF
 * 
 * @author TIS
 */
@Published(tag = "architect")
public interface HttpOutputStreamWriter {

    /**
     * Streamに書き出しを行わせるためのメソッド。
     * @param outputStream 書き込み対象Stream
     * @throws IOException 書き込み例外
     */
    void writeStream(OutputStream outputStream) throws IOException;

}
