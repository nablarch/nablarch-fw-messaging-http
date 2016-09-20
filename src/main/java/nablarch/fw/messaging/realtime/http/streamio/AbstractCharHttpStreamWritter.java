package nablarch.fw.messaging.realtime.http.streamio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import nablarch.core.util.FileUtil;
import nablarch.core.util.annotation.Published;

/**
 * HTTP通信時に使用するWritterクラス。
 * 
 * @author TIS
 */
@Published(tag = "architect")
public abstract class AbstractCharHttpStreamWritter implements
        HttpOutputStreamWriter {

    /** 出力時に使用するキャラクターセット*/
    private String charset;

    /**
     * コンストラクタ。
     */
    public AbstractCharHttpStreamWritter() {
        super();
    }

    /**
     * コンストラクタ。
     * @param charset キャラクターセット
     */
    public AbstractCharHttpStreamWritter(String charset) {
        super();
        this.charset = charset;
    }

    /**
     * ストリームに出力する。
     * @param outputStream 出力先ストリームの実体
     * @throws IOException ストリームへの読み書きに失敗した際、送出されることがある。
     */
    public void writeStream(OutputStream outputStream) throws IOException {
        BufferedWriter bw = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            if (getCharset() == null) {
                outputStreamWriter = new OutputStreamWriter(outputStream);
            } else {
                outputStreamWriter = new OutputStreamWriter(outputStream, getCharset());
            }
            bw = new BufferedWriter(outputStreamWriter);

            for (int i = 0; i < getWriteTarget().size(); i++) {
                bw.write(getWriteTarget().get(i));
                if (i < getWriteTarget().size() - 1) {
                    //最終行以外は、行区切りを出力する。
                    bw.write(getLineSeparator());
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            FileUtil.closeQuietly(bw);
        }
    }

    /**
     * 出力時に使用するキャラクターセットを取得する。
     * @return キャラクターセット
     */
    public String getCharset() {
        return charset;
    }
    
    /**
     * 行の区切りを取得する。
     * @return 行の区切り
     */
    public String getLineSeparator() {
        return "";
    }
    
    /**
     * 書き込み対象の文字列を取得する。
     * @return 書き込み対象文字列
     */
    public abstract List<String> getWriteTarget();
}
