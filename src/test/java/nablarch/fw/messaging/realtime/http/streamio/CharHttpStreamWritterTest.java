package nablarch.fw.messaging.realtime.http.streamio;

import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CharHttpStreamWritterTest {

    /**
     * 引数で渡したStreamに書き込めること。
     */
    @Test
    public void testWriteStream() throws Exception{
        ByteArrayOutputStream byteArrayOutputStream = null;
        try{
            CharHttpStreamWritter charHttpStreamWritter = new CharHttpStreamWritter("UTF-8");
            charHttpStreamWritter.append("あいう\nえお");
            charHttpStreamWritter.append(" かきく\nけこ ");
            byteArrayOutputStream = new ByteArrayOutputStream();;
            charHttpStreamWritter.writeStream(byteArrayOutputStream);
            assertThat(byteArrayOutputStream.toString("UTF-8"), is("あいう\nえお かきく\nけこ "));
        }finally{
            byteArrayOutputStream.close();
        }
        
        try{
            CharHttpStreamWritter charHttpStreamWritter = new CharHttpStreamWritter();
            charHttpStreamWritter.append("abc");
            charHttpStreamWritter.append("def");
            byteArrayOutputStream = new ByteArrayOutputStream();;
            charHttpStreamWritter.writeStream(byteArrayOutputStream);
            assertThat(byteArrayOutputStream.toString(), is("abcdef"));
        }finally{
            byteArrayOutputStream.close();
        }
    }
}
