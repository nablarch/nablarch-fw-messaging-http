package nablarch.fw.messaging.realtime.http.streamio;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP通信時に使用するReaderクラス。
 * 
 * @author Masaya Seko
 */
public class CharHttpStreamWritter extends AbstractCharHttpStreamWritter {
    /** 出力する文字列リスト*/
    private List<String> outputStringList = new ArrayList<String>();
    
    /**
     * コンストラクタ。
     */
    public CharHttpStreamWritter() {
        super();
    }

    /**
     * コンストラクタ。
     * @param charset 文字コード
     */
    public CharHttpStreamWritter(String charset) {
        super(charset);
    }

    @Override
    public List<String> getWriteTarget() {
        return outputStringList;
    }
    
    /**
     * 書き込み対象の文字列を追加する。
     * @param st 書き込み対象の文字列
     */
    public void append(String st) {
        outputStringList.add(st);
    }
}
