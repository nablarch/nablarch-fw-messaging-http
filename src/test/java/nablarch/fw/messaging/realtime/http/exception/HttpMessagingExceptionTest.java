package nablarch.fw.messaging.realtime.http.exception;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class HttpMessagingExceptionTest {
    /**
     * getMessage()で得たメッセージに、追加情報が含まれていることを確認する。
     */
    @Test
    public void testGetMessage() throws Exception{
        HttpMessagingException exception = new HttpMessagingException("test message");
        exception.setTargetUrl("http://a.com");
        exception.setStatusCode(500);
        assertThat(exception.getMessage(), is("test message URL=[http://a.com]. status code=[500]."));
    }

    /**
     * {@link HttpMessagingException#HttpMessagingException()}のテスト。
     */
    @Test
    public void testDefaultConstructor() throws Exception{
        HttpMessagingException exception = new HttpMessagingException();
        assertThat(exception.getMessage(), is(nullValue()));
    }

    /**
     * {@link HttpMessagingException#HttpMessagingException(String, Integer, Throwable))}のテスト。
     */
    @Test
    public void testConstructorWithTargetUrlAndStatusCodeAndCause() throws Exception{

        Throwable th = null;

        HttpMessagingException exception = new HttpMessagingException(null, null, th);
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));

        exception = new HttpMessagingException("url_test", null, th);
        assertThat(exception.getMessage(), is(" URL=[url_test]."));
        assertThat(exception.getCause(), is(nullValue()));

        exception = new HttpMessagingException("url_test", Integer.valueOf(200), th);
        assertThat(exception.getMessage(), is(" URL=[url_test]. status code=[200]."));
        assertThat(exception.getCause(), is(nullValue()));

        exception = new HttpMessagingException(null, Integer.valueOf(200), th);
        assertThat(exception.getMessage(), is(" status code=[200]."));
        assertThat(exception.getCause(), is(nullValue()));

        th = new IllegalArgumentException("test2");

        exception = new HttpMessagingException("url_test", Integer.valueOf(200), th);
        assertThat(exception.getMessage(), is(th.toString() + " URL=[url_test]. status code=[200]."));
        assertThat(exception.getCause().getMessage(), is("test2"));

        exception = new HttpMessagingException(null, Integer.valueOf(200), th);
        assertThat(exception.getMessage(), is(th.toString() + " status code=[200]."));
        assertThat(exception.getCause().getMessage(), is("test2"));

        exception = new HttpMessagingException(null, null, th);
        assertThat(exception.getMessage(), is(th.toString()));
        assertThat(exception.getCause().getMessage(), is("test2"));
    }

    /**
     * {@link HttpMessagingException#HttpMessagingException(String, Throwable)}のテスト。
     */
    @Test
    public void testConstructorWithMessageAndCause() throws Exception{

        HttpMessagingException exception = new HttpMessagingException(null, null);
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));

        exception = new HttpMessagingException(null, new IllegalArgumentException("test1"));
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause().getMessage(), is("test1"));

        exception = new HttpMessagingException("msg_test", null);
        assertThat(exception.getMessage(), is("msg_test"));
        assertThat(exception.getCause(), is(nullValue()));
    }

    /**
     * {@link HttpMessagingException#HttpMessagingException(Throwable)}のテスト。
     */
    @Test
    public void testConstructorWithCause() throws Exception{

        Throwable th = null;

        HttpMessagingException exception = new HttpMessagingException(th);
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));

        th = new IllegalArgumentException("test1");
        exception = new HttpMessagingException(th);
        assertThat(exception.getMessage(), is(th.toString()));
        assertThat(exception.getCause().getMessage(), is("test1"));
    }
}
