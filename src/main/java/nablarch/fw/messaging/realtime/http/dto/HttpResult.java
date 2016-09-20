package nablarch.fw.messaging.realtime.http.dto;

import java.util.List;
import java.util.Map;

import nablarch.core.util.annotation.Published;

/**
 * HTTP通信の結果を格納するためのクラス。
 * 
 * @author TIS
 */
@Published(tag = "architect")
public class HttpResult {

    /** レスポンスコード*/
    private Integer responseCode;

    /** 受信内容(StramReaderの読み取り結果格納用)*/
    private Object readObject;

    /** Httpヘッダ*/
    private Map<String, List<String>> headerInfo;

    /**
     * Httpヘッダを取得する。
     * @return Httpヘッダ
     */
    public Map<String, List<String>> getHeaderInfo() {
        return headerInfo;
    }

    /**
     * コンストラクタ。
     */
    public HttpResult() {
        super();
    }
    
    /**
     * レスポンスコードを取得する。
     * @return responseCode
     */
    public Integer getResponseCode() {
        return responseCode;
    }

    /**
     * レスポンスコードを設定する。
     * @param responseCode レスポンスコード
     */
    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * 受信内容を返却する。
     * @return readObject
     */
    public Object getReadObject() {
        return readObject;
    }

    /**
     * 受信内容を設定する。
     * @param readObject 受信内容
     */
    public void setReadObject(Object readObject) {
        this.readObject = readObject;
    }

    /**
     * ヘッダー情報を設定する
     * @param headerInfo ヘッダ情報
     */
    public void setHeaderInfo(Map<String, List<String>> headerInfo) {
        this.headerInfo = headerInfo;
    }
}
