package nablarch.fw.messaging.realtime.http.streamio;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nablarch.core.util.FileUtil;

import org.junit.Test;

public class CharHttpStreamReaderTest extends CharHttpStreamReader {

    /**
     * 引数で渡したStreamから読み込めること。
     */
    @Test
    public void testReadStream() throws Exception{
        ByteArrayInputStream byteArrayInputStream = null;
        try{
            byteArrayInputStream = new ByteArrayInputStream(" あいう\nえお ".getBytes("UTF-8"));
            
            CharHttpStreamReader charHttpStreamReader = new CharHttpStreamReader();
            Map<String, List<String>> headerFields = new TreeMap<String, List<String>>();
            headerFields.put("content-type", Collections.singletonList("application/json; charset=UTF-8"));
            headerFields.put("X-x", Collections.singletonList("a"));
            
            charHttpStreamReader.setHeaderInfo(headerFields);
            
            Object readObject = charHttpStreamReader.readInputStream(byteArrayInputStream);
            assertThat((String)readObject, is(" あいう\nえお "));
        }finally{
            FileUtil.closeQuietly(byteArrayInputStream);
        }

        try{
            byteArrayInputStream = new ByteArrayInputStream("abc".getBytes("ISO_8859_1"));
            
            CharHttpStreamReader charHttpStreamReader = new CharHttpStreamReader();
            
            Object readObject = charHttpStreamReader.readInputStream(byteArrayInputStream);
            assertThat((String)readObject, is("abc"));
        }finally{
            byteArrayInputStream.close();
        }

        
    }

}
