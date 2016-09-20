package nablarch.fw.messaging.realtime.http.streamio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nablarch.core.util.FileUtil;

/**
 * HTTP通信時に使用するReaderクラス。
 * 
 * @author Masaya Seko
 */
public class CharHttpStreamReader extends AbstractCharHttpStreamReader {

    /** データ読み込み時のバッファサイズ */
    private static final int READ_BUF_SIZE = 8192;
    
    /**
     * {@inheritDoc} <br>
     */
    public Object readInputStream(InputStream is) throws IOException {

        InputStreamReader isr = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[READ_BUF_SIZE];
        try {
            isr = new InputStreamReader(is, getEncode());
            br = new BufferedReader(isr);
            int len;
            while ((len = br.read(buf)) > 0) {
                sb.append(buf, 0, len);
            }
            
        } finally {
            FileUtil.closeQuietly(br);
        }
        return sb.toString();
    }

}
