package nablarch.fw.messaging.realtime.http.streamio;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
            List<String> list = null;
            list = new ArrayList<String>();
            list.add("application/json; charset=UTF-8");
            headerFields.put("Content-Type", list);
            list = new ArrayList<String>();
            list.add("a");
            headerFields.put("X-x", list);
            
            charHttpStreamReader.setHeaderInfo(headerFields);
            
            Object readObject = charHttpStreamReader.readInputStream(byteArrayInputStream);
            assertThat((String)readObject, is(" あいう\nえお "));
        }finally{
            byteArrayInputStream.close();
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
