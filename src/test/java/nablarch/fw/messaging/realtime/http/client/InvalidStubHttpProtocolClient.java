package nablarch.fw.messaging.realtime.http.client;

import nablarch.fw.messaging.realtime.http.dto.HttpResult;
import nablarch.fw.messaging.realtime.http.streamio.HttpInputStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.HttpOutputStreamWriter;

import javax.net.ssl.SSLContext;
import java.util.List;
import java.util.Map;

public class InvalidStubHttpProtocolClient implements HttpProtocolClient {

    @Override
    public void setProxyInfo(String url, int port) {
        // TODO 自動生成されたメソッド・スタブ
        
    }

    @Override
    public void setSslContext(SSLContext sslContext) {
        // TODO 自動生成されたメソッド・スタブ
        
    }

    @Override
    public void setConnectTimeout(int connectTimeout) {
        // TODO 自動生成されたメソッド・スタブ
        
    }

    @Override
    public void setReadTimeout(int httpReadTimeout) {
        // TODO 自動生成されたメソッド・スタブ
        
    }

    @Override
    public void setContentType(String contentType) {
        // TODO 自動生成されたメソッド・スタブ
        
    }

    @Override
    public void setAccept(String accept) {
        // TODO 自動生成されたメソッド・スタブ
        
    }

    @Override
    public void setQueryStringEncoding(String queryStringEncoding) {
        // TODO 自動生成されたメソッド・スタブ
        
    }

    @Override
    public HttpResult execute(HttpRequestMethodEnum httpMethod, String url,
            Map<String, List<String>> headerInfo,
            Map<String, String> urlParams, HttpOutputStreamWriter writer,
            HttpInputStreamReader reader) {
        // TODO 自動生成されたメソッド・スタブ
        return null;
    }
}
