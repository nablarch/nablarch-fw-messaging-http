package nablarch.fw.messaging.handler;


import nablarch.fw.Result.Error;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.HttpRequestJavaPackageMapping;

import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.web.servlet.MockServletRequest;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HttpMessagingErrorHandlerTest {
    
    private HttpServletRequest createServletRequest(String uri) {
        MockServletRequest request = new MockServletRequest();
        request.setRequestURI(uri);
        request.setContextPath("");
        request.setMethod("POST");
        return request;
    }
    
    private ServletExecutionContext createExecutionContext(HttpServletRequest servletReq) {
        ServletExecutionContext ctx = new ServletExecutionContext(servletReq, null, null);
        ctx.setHandlerQueue(Collections.singletonList(
                new HttpRequestJavaPackageMapping("/", "nablarch.fw.web.handler.test")));
        return ctx;
    }

    /**
     * 後続のハンドラで例外が発生しなかった場合は、
     * 後続のハンドラが返したレスポンスがそのまま返されること。
     */
    @Test
    public void testNormal() {
        
        // レスポンスのコンテンツパスが指定された場合
        
        HttpServletRequest servletReq = createServletRequest("/NormalHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
       
        HttpMessagingErrorHandler handler = new HttpMessagingErrorHandler();
        
        request.setParam("code", "200");
        request.setParam("path", "/success.jsp");
        
        OnMemoryLogWriter.clear();
        HttpResponse response = handler.handle(request, context);

        assertThat("正常レスポンスなので障害ログは出力されないこと",
                filterLog(OnMemoryLogWriter.getMessages("writer.appLog"), "FATAL").size(), is(0));
        
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentPath().getPath(), is("/success.jsp"));
        
        // レスポンスのコンテンツパスが指定されなかった場合
        
        servletReq = createServletRequest("/NormalHandler/index.html");
        context = createExecutionContext(servletReq);
        request = context.getHttpRequest();
        handler = new HttpMessagingErrorHandler();
        handler.setDefaultPage("400", "/test_userError.jsp");
        
        request.setParam("code", "400");
        
        response = handler.handle(request, context);

        assertThat("400系のエラーなので障害ログは出力されない。",
                filterLog(OnMemoryLogWriter.getMessages("writer.appLog"), "FATAL").size(), is(0));

        assertThat(response.getStatusCode(), is(400));
        assertThat(response.getContentPath().getPath(), is("/test_userError.jsp"));
    }
    
    /**
     * 後続のハンドラで{@link nablarch.fw.NoMoreHandlerException}が発生した場合は、
     * 404エラーがレスポンスされること。
     */
    @Test
    public void testNoMoreHandlerException() {
        
        // デフォルトページを設定しない場合
        
        HttpServletRequest servletReq = createServletRequest("/UnknownHandler/index.html");
        ServletExecutionContext context = new ServletExecutionContext(servletReq, null, null);
        HttpRequest request = context.getHttpRequest();
        HttpMessagingErrorHandler handler = new HttpMessagingErrorHandler();
        OnMemoryLogWriter.clear();

        HttpResponse response = handler.handle(request, context);
        
        assertThat(OnMemoryLogWriter.getMessages("writer.appLog").toString(),
                containsString("There were no Handlers in handlerQueue. uri = [/UnknownHandler/index.html]"));
        assertThat(response.getStatusCode(), is(404));
        assertThat(response.getContentPath(), is(nullValue()));
        
        // デフォルトページを設定した場合
        
        servletReq = createServletRequest("/UnknownHandler/index.html");
        context = new ServletExecutionContext(servletReq, null, null);
        request = context.getHttpRequest();
        handler = new HttpMessagingErrorHandler();
        handler.setDefaultPage("404", "/test_404.jsp");
        OnMemoryLogWriter.clear();
        
        response = handler.handle(request, context);
        
        assertThat(OnMemoryLogWriter.getMessages("writer.appLog").toString(),
                   containsString("There were no Handlers in handlerQueue. uri = [/UnknownHandler/index.html]"));
        assertThat(response.getStatusCode(), is(404));
        assertThat(response.getContentPath().getPath(), is("/test_404.jsp"));
    }
    
    /**
     * 後続のハンドラで{@link Error}が発生した場合は、
     * {@link Error}に応じたレスポンスされること。
     */
    @Test
    public void testResultError() {
        
        HttpServletRequest servletReq = createServletRequest("/ResultErrorHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpMessagingErrorHandler handler = new HttpMessagingErrorHandler();
        handler.setDefaultPage("500", "/test_systemError.jsp");
        
        OnMemoryLogWriter.clear();
        HttpResponse response = handler.handle(request, context);

        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getContentPath().getPath(), is("/test_systemError.jsp"));
        
        // 期待するメッセージがログ出力せれていることをアサート
        List<String> messages = filterLog(OnMemoryLogWriter.getMessages("writer.appLog"), "FATAL");
        assertThat("出力される障害ログは1つであること", messages.size(), is(1));
        assertThat("InternalErrorが出力されていること", messages.get(0), containsString("[500 InternalError]"));
        assertThat("デフォルトのログメッセージが出力されていること", messages.get(0), containsString("The request could not be processed due to a unexpected condition. please contact our support team if you need."));
    }

    /**
     * 後続のハンドラで{@link Error}が発生した場合のテスト。
     * 任意の設定を行った場合のログ出力テスト。
     *
     * ここでは、障害通知ログの出力対象のステータスコードを200と503としているため、
     * その2つのステータスコードのみログ出力されることを確認する。
     */
    @Test
    public void testWriterFailureLogFromOtherSetting() throws NoSuchFieldException, IllegalAccessException {

        HttpMessagingErrorHandler handler = new HttpMessagingErrorHandler();
        handler.setWriteFailureLogPattern("200|503");
        handler.setDefaultPages(new HashMap<String, String>() {
            {
                put("1..", "/jsp/systemError.jsp");
                put("2..", "/jsp/systemError.jsp");
                put("3..", "/jsp/systemError.jsp");
                put("4..", "/jsp/systemError.jsp");
                put("5..", "/jsp/systemError.jsp");
                put("6..", "/jsp/systemError.jsp");
                put("7..", "/jsp/systemError.jsp");
                put("8..", "/jsp/systemError.jsp");
                put("9..", "/jsp/systemError.jsp");
                put("503", "/jsp/userError.jsp");
            }
        });

        Field codeField = HttpResponse.Status.class.getDeclaredField("code");
        codeField.setAccessible(true);
        for (HttpResponse.Status status : HttpResponse.Status.values()) {
            int statusCode = (Integer) codeField.get(status);
            System.out.println("################################################## " + statusCode + " ##################################################");
            HttpServletRequest servletReq = createServletRequest("/StatusTestHandler/index.html");
            ServletExecutionContext context = createExecutionContext(servletReq);
            HttpRequest request = context.getHttpRequest();
            request.getParamMap().put("statusCode", new String[]{String.valueOf(statusCode)});

            OnMemoryLogWriter.clear();
            HttpResponse response = handler.handle(request, context);

            assertThat(response.getStatusCode(), is(statusCode));
            if (statusCode == 503) {
                assertThat(response.getContentPath().getPath(), is("/jsp/userError.jsp"));
            } else {
                assertThat(response.getContentPath().getPath(), is("/jsp/systemError.jsp"));
            }

            List<String> messages = OnMemoryLogWriter.getMessages("writer.appLog");
            final List<String> fatalLog = filterLog(messages, "FATAL");
            if (statusCode == 200 || statusCode == 503) {
                // ステータスコードが503以外の場合は、障害通知ログが出力されていること。
                assertThat("出力される障害ログは1つであること", fatalLog.size(), is(1));
                assertThat("Errorが出力されていること", fatalLog.get(0), containsString("Error"));
            } else {
                assertThat("障害通知ログは出力されないこと", fatalLog.size(), is(0));
            }
        }
    }

    /**
     * 後続のハンドラで{@link HttpErrorResponse}が発生した場合は、
     * その元例外にもとづいたログ出力を行ったのち、通常レスポンスとしてリターンする。
     */
    @Test
    public void testHttpErrorResponse() {
        
        HttpServletRequest servletReq = createServletRequest("/HttpErrorResponseHandlerCausedByNPE/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpMessagingErrorHandler handler = new HttpMessagingErrorHandler();

        OnMemoryLogWriter.clear();
        HttpResponse response = handler.handle(request, context);
        
        List<String> messages = filterLog(OnMemoryLogWriter.getMessages("writer.appLog"), "FATAL");
        assertThat("出力される障害ログは1つであること", messages.size(), is(1));

        assertThat(response.getStatusCode(), is(400));
        assertThat(response.getContentPath().getPath(), is("/test_400.jsp"));
    }

    private List<String> filterLog(List<String> logs, String filterMessage) {
        List<String> result = new ArrayList<String>();
        for (String log : logs) {
            if (log.contains(filterMessage)) {
                result.add(log);
            }
        }
        return result;
    }
}