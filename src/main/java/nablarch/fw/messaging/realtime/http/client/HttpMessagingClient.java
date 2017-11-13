package nablarch.fw.messaging.realtime.http.client;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SimpleDataConvertResult;
import nablarch.core.dataformat.SimpleDataConvertUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.MessageSenderClient;
import nablarch.fw.messaging.MessageSenderSettings;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.messaging.logging.MessagingLogUtil;
import nablarch.fw.messaging.realtime.http.client.HttpProtocolClient.HttpRequestMethodEnum;
import nablarch.fw.messaging.realtime.http.dto.HttpResult;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingInvalidDataFormatException;
import nablarch.fw.messaging.realtime.http.streamio.CharHttpStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.CharHttpStreamWritter;
import nablarch.fw.messaging.realtime.http.streamio.HttpInputStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.HttpOutputStreamWriter;

/**
 * HTTPを利用したメッセージング機能の実装。
 * 
 * @author Masaya Seko
 */
@Published(tag = "architect")
public class HttpMessagingClient implements MessageSenderClient {
    /** 証跡ログを出力するロガー */
    private static final Logger MESSAGING_LOGGER = LoggerManager.get("MESSAGING");
    
    /** SyncMessageのヘッダレコードからステータスコードを取り出すために使用するキー */
    public static final String SYNCMESSAGE_STATUS_CODE = "STATUS_CODE";

    /** HTTPヘッダ名・メッセージID */
    private static final String HTTP_HEADER_MESSAGE_ID = "X-Message-Id";
    
    /** 応答電文のデータフォーマット定義ファイル名パターン  */
    private String responseMessageFormatFileNamePattern = "%s" + "_RECEIVE";
    
    /** 要求電文のフォーマット定義ファイル名のパターン */
    private String requestMessageFormatFileNamePattern = "%s" + "_SEND";
    
    /** メッセージ定義から取得したユーザIDを、フォーマット定義ファイルのどのキーと紐付けるか(コンポーネント定義ファイルからの設定を想定した変数)。*/
    private String userIdToFormatKey = null;

    /**送信にbody部が存在するHTTPメソッド*/
    private static final List<String> EXIST_BODY_HTTP_METHOD = Arrays.asList("POST", "PUT");

    /** クエリストリングをエンコードする際に使用するエンコーディング*/
    private String queryStringEncoding = "UTF-8";

    /** Content-Typeヘッダから文字セットを取得するためのパターン */
    private static final Pattern CHARSET_PTN = Pattern.compile(".*charset=(.+)");
    
    /**
     * HTTPを使用したリアルタイム通信通信を行う。
     * @param settings {@link nablarch.fw.messaging.MessageSender}の設定情報
     * @param requestMessage 要求電文
     * @return 応答電文
     * @throws MessagingException 通信に失敗した際に送出される。
     */
    @SuppressWarnings("unchecked")
    public SyncMessage sendSync(MessageSenderSettings settings, SyncMessage requestMessage) throws MessagingException {
        //settingsから送信内容生成に必要な情報を取り出し
        HttpRequestMethodEnum httpMethod = null;
        try {
            httpMethod = HttpRequestMethodEnum.valueOf(settings.getHttpMethod().toUpperCase());
            
        } catch (IllegalArgumentException e) {
            //サポートしないHTTPメソッド
            throw new MessagingException(String.format("%s is unsupported HTTP method.", settings.getHttpMethod().toUpperCase()), e);
        }
        String preUri = settings.getUri();

        
        //要求電文に、共通プロトコルヘッダ相当部分及びフレームワーク制御ヘッダ部で使用する要素を追加する
        addCommonValue(httpMethod, settings, requestMessage);
        
        //要求電文を元に送信内容を生成
        String uri = mapToUriString(preUri, httpMethod, requestMessage);
        Map<String, String> urlParams = mapToQueryMap(preUri, httpMethod, requestMessage);
        Map<String, List<String>> headerInfo = mapToHeaderMap(requestMessage);
        SimpleDataConvertResult bodyDataConvertResult = mapToBodyString(uri, httpMethod, requestMessage);
        
        String mimeType = getRequestContentsType(httpMethod, bodyDataConvertResult);
        HttpProtocolClient httpProtocolClient = createHttpProtocolClient();
        initHttpProtocolClient(httpProtocolClient, settings, mimeType);

        String charset = "UTF-8";
        String bodyText = "";
        if (bodyDataConvertResult != null) {
            charset = bodyDataConvertResult.getCharset().toString();
            bodyText = bodyDataConvertResult.getResultText();
        }

        // 証跡ログ
        if (MESSAGING_LOGGER.isInfoEnabled()) {
            emitRequestLog(requestMessage.getHeaderRecord(), httpMethod, uri, bodyText, charset);
        }
        
        //送信を実行
        HttpResult httpResult = execute(httpProtocolClient, httpMethod, uri, headerInfo, urlParams, charset, bodyText);

        //応答電文を生成
        SyncMessage responseMessage = new SyncMessage(requestMessage.getRequestId());
        Map<String, Object> resHeadderMap = headerToMap(requestMessage, httpResult);
        
        // 証跡ログ
        if (MESSAGING_LOGGER.isInfoEnabled()) {
            emitResponseLog(resHeadderMap, getResponseBody(httpResult), getResponseCharset(resHeadderMap));
        }
        
        SimpleDataConvertResult resBodyDataConvertResult = bodyStringToMap(uri, httpMethod, requestMessage, httpResult);
        responseMessage.setHeaderRecord(resHeadderMap);
        Map<String, Object> responseData = null;
        if (resBodyDataConvertResult != null) {
            //応答にBody部分が存在した場合は、Mapへの変換が行われるためこちらの分岐が実行される。
            responseData = (Map<String, Object>) resBodyDataConvertResult.getResultMap();
        } else {
            responseData = new TreeMap<String, Object>();
        }
        responseMessage.addDataRecord(responseData);

        return responseMessage;
    }

    /**
     * 要求電文に、共通プロトコルヘッダ相当部分及びフレームワーク制御ヘッダ部で使用する要素を追加する。
     * @param httpMethod HTTPメソッド
     * @param settings {@link nablarch.fw.messaging.MessageSender}の設定情報
     * @param requestMessage 要求電文
     */
    protected void addCommonValue(HttpRequestMethodEnum httpMethod, MessageSenderSettings settings, SyncMessage requestMessage) {
        //HTTPヘッダに設定する追加情報を設定
        if (settings.getHttpMessageIdGenerator() != null) {
            String messageId = settings.getHttpMessageIdGenerator().generateId();
            if (messageId != null) {
                requestMessage.getHeaderRecord().put(HTTP_HEADER_MESSAGE_ID, messageId);
            }
        }
        
        if (getExistBodyHttpMethod().contains(httpMethod.toString())) {
            //本文が存在するHTTPメソッドの場合に処理する。
            //本文が存在するHTTPメソッドの場合、何かフレームワーク側で何か値を差し込まない場合でも、requestMessageのdataRecode部にはMapを用意しておく。
            //(後続処理のため)
            
            Map<String, Object> dataRecord = requestMessage.getDataRecord();
            if (dataRecord == null) {
                //Body部が設定されていない場合は、空のBody部を設定する。
                dataRecord = new TreeMap<String, Object>();
                requestMessage.addDataRecord(dataRecord);
            }
            if (!StringUtil.isNullOrEmpty(getUserIdToFormatKey())) {
                //「メッセージ定義から読み込んだユーザIDを、フォーマット定義のどのキーに対応させるか」がコンポーネント定義に書いてあれば、処理を行う。
                if (!dataRecord.containsKey(getUserIdToFormatKey())) {
                    //呼び出し側で、ユーザIDを設定していなければ、設定する。
                    if (!StringUtil.isNullOrEmpty(settings.getHttpMessagingUserId())) {
                        dataRecord.put(getUserIdToFormatKey(), settings.getHttpMessagingUserId());
                    }
                }
            }
        }
    }

    /**
     * HTTPプロトコルを実装したクラスのインスタンスを生成する。
     * @return HTTPプロトコルを用いた通信を行うクラスのインスタンス
     */
    protected HttpProtocolClient createHttpProtocolClient() {
        return new HttpProtocolBasicClient();
    }

    /**
     * HTTPプロトコルを実装したクラスのインスタンスの初期化を行う。
     * @param argHttpProtocolClient 初期化対象のHttpProtocolClientのインスタンス
     * @param settings {@link nablarch.fw.messaging.MessageSender}の設定情報
     * @param mimeType 送信するデータの種別
     */
    protected void initHttpProtocolClient(HttpProtocolClient argHttpProtocolClient, MessageSenderSettings settings, String mimeType) {
        if (settings.getSslContextSettings() != null) {
            SSLContext sslContext = settings.getSslContextSettings().getSSLContext();
            if (sslContext != null) {
                argHttpProtocolClient.setSslContext(settings.getSslContextSettings().getSSLContext());
            }
        }
        
        if (settings.getHttpProxyHost() != null) {
            argHttpProtocolClient.setProxyInfo(settings.getHttpProxyHost(), settings.getHttpProxyPort());
        }
        argHttpProtocolClient.setConnectTimeout(settings.getHttpConnectTimeout());
        argHttpProtocolClient.setReadTimeout(settings.getHttpReadTimeout());
        argHttpProtocolClient.setContentType(mimeType);
        argHttpProtocolClient.setAccept(getAccept());
        argHttpProtocolClient.setQueryStringEncoding(getQueryStringEncoding());
    }

    /**
     * レスポンスの本文として受信可能なタイプを取得します
     * @return レスポンスの本文として受信可能なタイプ
     */
    protected String getAccept() {
        return "text/json, text/xml";
    }

    /**
     * URIを生成する。
     * <p>
     * このメソッドはURI生成ルールのカスタマイズのために存在している。本クラスの実装では、引数preUriを返却する。
     * </p>
     * @param preUri メッセージ送信定義に記述されているURI
     * @param httpMethod HTTPメソッド
     * @param requestMessage 要求電文
     * @return URI
     */
    protected String mapToUriString(String preUri, HttpRequestMethodEnum httpMethod, SyncMessage requestMessage) {
        return preUri;
    }

    /**
     * クエリストリングを生成する。
     * <p>
     * このメソッドはクエリストリング生成ルールのカスタマイズのために存在している。本クラスの実装では、空のMapを返す。
     * </p>
     * @param preUri メッセージ送信定義に記述されているURI
     * @param httpMethod HTTPメソッド
     * @param requestMessage 要求電文
     * @return URI
     */
    protected Map<String, String> mapToQueryMap(String preUri, HttpRequestMethodEnum httpMethod, SyncMessage requestMessage) {
        return new HashMap<String, String>();
    }

    /**
     * HTTPヘッダに含める内容を生成する。
     * @param requestMessage 要求電文
     * @return HTTPヘッダに含める内容を格納したMap
     */
    protected Map<String, List<String>> mapToHeaderMap(SyncMessage requestMessage) {
        Map<String, List<String>> ret = new HashMap<String, List<String>>();
        for (Map.Entry<String, Object> entry : requestMessage.getHeaderRecord().entrySet()) {
            List<String> value = new ArrayList<String>();
            if (entry.getValue() != null) {
                value.add(StringUtil.toString(entry.getValue()));
            } else {
                value.add("");
            }
            ret.put(entry.getKey(), value);
        }
        return ret;
    }
    
    /**
     * HTTP通信のボディ部を生成する。
     * @param uri 接続先
     * @param httpMethod HTTPメソッド
     * @param requestMessage 要求電文
     * @return 変換後の文字列
     * @throws HttpMessagingInvalidDataFormatException 電文フォーマット変換に失敗した場合に送出される。
     */
    protected SimpleDataConvertResult mapToBodyString(String uri, HttpRequestMethodEnum httpMethod, SyncMessage requestMessage)
            throws HttpMessagingInvalidDataFormatException {
        
        SimpleDataConvertResult ret = null;
        if (getExistBodyHttpMethod().contains(httpMethod.toString())) {
            //Body部が存在するHTTPメソッドについては、本文について電文変換を実施する。
            Map<String, Object> dataRecord = requestMessage.getDataRecord();
            try {
                if (dataRecord.size() > 0) {
                    //電文フォーマット変換対象のデータが存在していれば、変換を行う。
                    String formatName = String.format(getRequestMessageFormatFileNamePattern(), requestMessage.getRequestId());
                    ret = SimpleDataConvertUtil.buildData(formatName, dataRecord);
                }
            } catch (InvalidDataFormatException e) {
                String message = "Invalid request message format. requestId=[" + requestMessage.getRequestId() + "].";
                throw new HttpMessagingInvalidDataFormatException(message, uri, dataRecord, e);
            }
        }
        return ret;
    }

    /**
     * 送信時に設定するコンテンツタイプを取得する。
     * @param httpMethod HTTPメソッド
     * @param requestBodyDataConvertResult 本文のデータ変換結果
     * @return コンテンツタイプ
     */
    protected String getRequestContentsType(HttpRequestMethodEnum httpMethod, SimpleDataConvertResult requestBodyDataConvertResult) {
        String contentType = "text/plain";
        Charset charset = Charset.forName("UTF-8");
        if (getExistBodyHttpMethod().contains(httpMethod.toString())) {
            //Body部が存在するHTTPメソッドについては、コンテンツタイプを取得する。
            if (requestBodyDataConvertResult != null) {
                //電文フォーマット変換の結果から、コンテンツタイプに必要な情報を取り出す。
                contentType = requestBodyDataConvertResult.getMimeType();
                charset = requestBodyDataConvertResult.getCharset();
            }
        }
        contentType = String.format("%s;charset=%s", contentType, charset.name());
        return contentType;
    }

    /**
     * HTTPリクエストを送出する。
     * @param httpProtocolClient HTTPリクエストを発行するオブジェクト
     * @param httpMethod HTTPメソッド
     * @param uri 送信先
     * @param headerInfo HTTPリクエストのヘッダ情報
     * @param urlParams URLパラメータ
     * @param charset 文字コード
     * @param bodyText HTTPリクエストの本文
     * @return 送信結果
     */
    protected HttpResult execute(HttpProtocolClient httpProtocolClient,
            HttpRequestMethodEnum httpMethod, String uri,
            Map<String, List<String>> headerInfo,
            Map<String, String> urlParams,
            String charset,
            String bodyText) {
        
        HttpResult httpResult = null;
        HttpInputStreamReader reader = createCharHttpStreamReader();
        HttpOutputStreamWriter writer = null;
        switch (httpMethod) {
        case GET:
            httpResult = httpProtocolClient.execute(httpMethod, uri, headerInfo, urlParams, null, reader);
            break;
        case POST:
            writer = createCharHttpStreamWritter(charset, bodyText);
            httpResult = httpProtocolClient.execute(httpMethod, uri, headerInfo, urlParams, writer, reader);
            break;
        case PUT:
            writer = createCharHttpStreamWritter(charset, bodyText);
            httpResult = httpProtocolClient.execute(httpMethod, uri, headerInfo, urlParams, writer, reader);
            break;
        case DELETE:
            httpResult = httpProtocolClient.execute(httpMethod, uri, headerInfo, urlParams, null, reader);
            break;
        default:
            //not reached
            break;
        }
        
        return httpResult;
    }

    /**
     * HTTPリクエストを発行後、OutputStreamを読み取り結果を返却させるためのIFを生成する。
     * @return OutputStreamを読み取り結果を返却させるためのIF
     */
    protected HttpInputStreamReader createCharHttpStreamReader() {
        return new CharHttpStreamReader();
        
    }

    /**
     * HTTPリクエストを発行時の送信内容を保持するオブジェクトを生成する。
     * @param charset 文字コード
     * @param bodyText 送信時の本文
     * @return 送信内容を表すオブジェクト
     */
    protected HttpOutputStreamWriter createCharHttpStreamWritter(String charset, String bodyText) {
        CharHttpStreamWritter writer = new CharHttpStreamWritter(charset);
        writer.append(bodyText);
        return writer;
    }
    
    /**
     * 返信のヘッダ部分を解析し、応答電文に設定するデータを生成する。
     * @param requestMessage 要求電文
     * @param httpResult 送信結果
     * @return 解析後のMap
     */
    protected Map<String, Object> headerToMap(SyncMessage requestMessage, HttpResult httpResult) {
        Map<String, Object> ret = new TreeMap<String, Object>();
        Map<String, List<String>> headerInfo = httpResult.getHeaderInfo();
        for (Entry<String, List<String>> entry : headerInfo.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                //keyがnullの場合、空文字列に置き換える。(ステータスラインの部分がkey=nullの状態で送信されてくるが、treeMapのキーにnullは使用できないため)
                key = "";
            }
            List<String> values = entry.getValue();
            //値が複数の場合、最初に取れた値を採用する。
            ret.put(key, values.get(0));
        }
        ret.put(SYNCMESSAGE_STATUS_CODE, Integer.toString(httpResult.getResponseCode()));
        return ret;
    }
    
    /**
     * 返信のボディ部分を解析し、応答電文に設定するデータを生成する。
     * @param uri 接続先
     * @param httpMethod HTTPメソッド
     * @param requestMessage 要求電文
     * @param httpResult 送信結果
     * @return 解析後のMap
     * @throws HttpMessagingInvalidDataFormatException 電文フォーマット変換に失敗した場合に送出される。
     */
    protected SimpleDataConvertResult bodyStringToMap(String uri, HttpRequestMethodEnum httpMethod, SyncMessage requestMessage, HttpResult httpResult) 
            throws HttpMessagingInvalidDataFormatException {
        SimpleDataConvertResult ret = null;
        String data = (String) httpResult.getReadObject();
        try {
            if (!StringUtil.isNullOrEmpty(data)) {
                //電文フォーマット変換対象のデータが存在していれば、変換を行う。
                String formatName = String.format(getResponseMessageFormatFileNamePattern(), requestMessage.getRequestId());
                ret = SimpleDataConvertUtil.parseData(formatName, data);
            }
        } catch (InvalidDataFormatException e) {
            String message = "Invalid receive message format. requestId=[" + requestMessage.getRequestId() + "].";
            throw new HttpMessagingInvalidDataFormatException(
                    message, uri, httpResult.getResponseCode(), httpResult.getHeaderInfo(), data, e);
        }
        return ret;
    }

    /**
     * 応答電文のデータフォーマット定義ファイル名パターンを取得する。
     * @return 応答電文のデータフォーマット定義ファイル名パターン
     */
    protected String getResponseMessageFormatFileNamePattern() {
        return responseMessageFormatFileNamePattern;
    }

    /**
     * 要求電文のフォーマット定義ファイル名のパターンを取得する。
     * @return 要求電文のフォーマット定義ファイル名のパターン
     */
    protected String getRequestMessageFormatFileNamePattern() {
        return requestMessageFormatFileNamePattern;
    }
    
    /**
     * ユーザIDとフォーマット定義ファイル上のキーとの対応を取得する。
     * @return フォーマット定義ファイル上のキー
     */
    public String getUserIdToFormatKey() {
        return userIdToFormatKey;
    }

    /**
     * ユーザIDとフォーマット定義ファイル上のキーとの対応を設定する。
     * @param userIdToFormatKey フォーマット定義ファイル上のキー
     */
    public void setUserIdToFormatKey(String userIdToFormatKey) {
        this.userIdToFormatKey = userIdToFormatKey;
    }
    
    /**
     * 送信にbody部が存在するHTTPメソッドのリストを取得する。
     * @return 送信にbody部が存在するHTTPメソッドのリスト
     */
    protected List<String> getExistBodyHttpMethod() {
        return EXIST_BODY_HTTP_METHOD;
    }

    /**
     * クエリストリングをエンコードする際に使用する文字コードを取得する。
     * @return 文字コード
     */
    public String getQueryStringEncoding() {
        return queryStringEncoding;
    }

    /**
     * クエリストリングをエンコードする際に使用する文字コードを設定する。
     * @param queryStringEncoding 文字コード
     */
    public void setQueryStringEncoding(String queryStringEncoding) {
        this.queryStringEncoding = queryStringEncoding;
    }

    /**
     * メッセージングの証跡ログを出力する。
     * @param requestHeader 要求ヘッダ情報
     * @param method HTTPメソッド
     * @param uri 接続先URI
     * @param bodyText 変換済みの要求メッセージ本文
     * @param charsetName 変換に使用した文字セット
     */
    private void emitRequestLog(Map<String, Object> requestHeader, HttpRequestMethodEnum method, String uri, final String bodyText, String charsetName) {
        // 共通のログ出力フォーマットとするためSendingMessageに変換する
        final Charset charset = Charset.forName(charsetName);
        SendingMessage sendingMessage = new SendingMessage() {
            @Override
            public byte[] getBodyBytes() {
                // ボディ部は変換済みテキストのバイト列を返却する
                return bodyText.getBytes(charset);
            }
        };
        
        sendingMessage.setHeaderMap(requestHeader);
        sendingMessage.setDestination(method + " " + uri);
        
        String log = MessagingLogUtil.getHttpSentMessageLog(sendingMessage, charset);
        
        MESSAGING_LOGGER.logInfo(log);
    }
    
    /**
     * メッセージングの証跡ログを出力する。
     * @param responseHeader 応答ヘッダ情報
     * @param bodyText 変換前の応答メッセージ本文
     * @param charsetName 変換に使用した文字セット
     */
    private void emitResponseLog(Map<String, Object> responseHeader, String bodyText, String charsetName) {
        // 共通のログ出力フォーマットとするためReceivedMessageに変換する
        Charset charset = Charset.forName(charsetName);
        byte[] bodyBytes = bodyText.getBytes(charset);
        
        ReceivedMessage receivedMessage = new ReceivedMessage(bodyBytes);
        
        receivedMessage.setHeaderMap(responseHeader);
        
        String log = MessagingLogUtil.getHttpReceivedMessageLog(receivedMessage, charset);
        
        MESSAGING_LOGGER.logInfo(log);
    }
    
    /**
     * 応答データの本文を取得する。
     * @param httpResult 応答データ
     * @return 応答データの本文
     */
    private String getResponseBody(HttpResult httpResult) {
        return StringUtil.nullToEmpty((String) httpResult.getReadObject());
    }
    
    /**
     * 応答データの文字セットを取得する。
     * @param resHeadderMap 応答データのヘッダ
     * @return Content-Typeヘッダに設定された文字セット、取得できない場合はデフォルト文字セット
     */
    private String getResponseCharset(Map<String, Object> resHeadderMap) {
        Charset charset = Charset.defaultCharset();
        String contentTypeHeader = StringUtil.nullToEmpty((String) resHeadderMap.get("Content-Type"));
        Matcher m = CHARSET_PTN.matcher(contentTypeHeader);
        if (m.find()) {
            charset = Charset.forName(m.group(1));
        }
        
        return charset.toString();
    }
    
}
