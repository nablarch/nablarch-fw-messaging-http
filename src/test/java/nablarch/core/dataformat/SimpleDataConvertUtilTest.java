package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * {@link SimpleDataConvertUtil}のテストを行います。
 *
 * @author TIS
 */
public class SimpleDataConvertUtilTest {

    @Rule
    public TestName testNameRule = new TestName();

    @BeforeClass
    public static void setUpClass() {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/SimpleDataConvertUtil.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);

    }

    private String getFormatFileName(String formatName) {
        return Builder.concat(
                   FilePathSetting.getInstance().getBasePathSettings().get("format").getPath(),
                   "/", formatName, ".",
                   FilePathSetting.getInstance().getFileExtensions().get("format")
               );

    }

    /**
     * テキストを指定しデータ解析を行う処理のテストを行います。<br>
     *
     * 条件：<br>
     *   テキストを指定するデータ解析処理を呼び出す。<br>
     *
     * 期待結果：<br>
     *   想定どおりの結果が返却されること。<br>
     */
    @Test
    public void testParseData() throws Exception {
        String formatName = testNameRule.getMethodName();

        // フォーマットファイル生成
        File formatFile = Hereis.file(getFormatFileName(formatName));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 key X
        *******/
        formatFile.deleteOnExit();

        // JSONデータ生成
        String data = Hereis.string();
        /*******
        {
          "key":"value"
        }
        *******/

        // 期待結果Map生成
        Map<String, Object> expectedMap = new HashMap<String, Object>() {
            private static final long serialVersionUID = -1245533158602021458L;
        {
            put("key", "value");
        }};
        String expectedDataType = "JSON";
        String expectedMimeType = "application/json";
        Charset expectedCharset = Charset.forName("UTF-8");

        // テスト実行
        SimpleDataConvertResult result = SimpleDataConvertUtil.parseData(formatName, data);

        // 結果検証
        assertEquals(expectedMap, result.getResultMap());
        assertEquals(expectedDataType, result.getDataType());
        assertEquals(expectedMimeType, result.getMimeType());
        assertEquals(expectedCharset, result.getCharset());
    }

    /**
     * データを構築し、テキストで返却を行う処理のテストを行います。<br>
     *
     * 条件：<br>
     *   テキストが返却されるデータ構築処理を呼び出す。<br>
     *
     * 期待結果：<br>
     *   想定どおりの結果が返却されること。<br>
     */
    @Test
    public void testBuildData() throws Exception {
        String formatName = testNameRule.getMethodName();

        // フォーマットファイル生成
        File formatFile = Hereis.file(getFormatFileName(formatName));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 key X
        *******/
        formatFile.deleteOnExit();

        // データMap生成
        Map<String, Object> dataMap = new HashMap<String, Object>() {
            private static final long serialVersionUID = -1245533158602021458L;
            {
                put("key", "value");
            }};

        // 期待結果JSONデータ生成
        String expectedData = Hereis.string().trim();
        /*******
        {"key":"value"}
        *******/

        String expectedDataType = "JSON";
        String expectedMimeType = "application/json";
        Charset expectedCharset = Charset.forName("UTF-8");

        // テスト実行
        SimpleDataConvertResult result = SimpleDataConvertUtil.buildData(formatName, dataMap);

        // 結果検証
        assertEquals(expectedData, result.getResultText());
        assertEquals(expectedDataType, result.getDataType());
        assertEquals(expectedMimeType, result.getMimeType());
        assertEquals(expectedCharset, result.getCharset());
    }

    /**
     * 標準的なフォーマッタサポート用抽象クラス({@link DataRecordFormatterSupport})を使用しないフォーマッタを使用した場合のテストを行います。<br>
     *
     * 条件：<br>
     *   単純に{@link DataRecordFormatter}を実装しただけのテスト用フォーマッタを使用する。<br>
     *
     * 期待結果：<br>
     *   結果に含まれるmimeTypeや文字コードセットがデフォルトのものとなること。<br>
     */
    @Test
    public void testNotUsingDefaultSupportFormatter() throws Exception {
        String formatName = testNameRule.getMethodName();

        // フォーマットファイル生成
        File formatFile = Hereis.file(getFormatFileName(formatName));
        /*******
        file-type:        "Hoge"
        text-encoding:    "UTF-8"
        [request]
        1 key X
        *******/
        formatFile.deleteOnExit();

        // JSONデータ生成
        String data = Hereis.string();
        /*******
        {
          "key":"value"
        }
        *******/

        Map<String, Object> expectedMap = null;
        String expectedDataType = null;
        String expectedMimeType = "text/plain";
        Charset expectedCharset = Charset.defaultCharset();

        // テスト実行
        SimpleDataConvertResult result = SimpleDataConvertUtil.parseData(formatName, data);

        // 結果検証
        assertEquals(expectedMap, result.getResultMap());
        assertEquals(expectedDataType, result.getDataType());
        assertEquals(expectedMimeType, result.getMimeType());
        assertEquals(expectedCharset, result.getCharset());
    }

    /**
     * 強制的にIOExceptionを発生させるテストを行います。<br>
     *
     * 条件：<br>
     *   読み込み時および書き込み時にIOExceptionを発生するのテスト用フォーマッタを使用する。<br>
     *
     * 期待結果：<br>
     *   nullが返却されること<br>
     */
    @Test
    public void testUsingIOExceptionFormatter() throws Exception {
        String formatName = testNameRule.getMethodName();

        // フォーマットファイル生成
        File formatFile = Hereis.file(getFormatFileName(formatName));
        /*******
        file-type:        "IoEx"
        text-encoding:    "UTF-8"
        [request]
        1 key X
         *******/
        formatFile.deleteOnExit();

        // JSONデータ生成
        String data = Hereis.string();
        /*******
        {
          "key":"value"
        }
         *******/
        Map<String, Object> mapData = new HashMap<String, Object>();
        mapData.put("key", "value");

        // テスト実行
        try {
            SimpleDataConvertUtil.parseData(formatName, data);
            fail();

        } catch(Throwable e) {
            assertTrue("IOExceptionをラップした実行時例外が送出されること", e.getCause() instanceof IOException);
        }

        try {
            SimpleDataConvertUtil.buildData(formatName, mapData);
            fail();

        } catch(Throwable e) {
            assertTrue("IOExceptionをラップした実行時例外が送出されること", e.getCause() instanceof IOException);
        }
    }

    /**
     * 強制的にインスタンス化を行います。<br>
     * （C0カバレッジ100%達成のため）
     *
     * 条件：<br>
     *   リフレクションによりプライベートコンストラクタを実行し強制的にインスタンス化を行う。<br>
     *
     * 期待結果：<br>
     *   インスタンスが生成されること。<br>
     */
    @Test
    public void testForceInstanciation() throws Exception {
        // テスト実行
        Constructor<SimpleDataConvertUtil> c = SimpleDataConvertUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        SimpleDataConvertUtil util = c.newInstance();

        // 結果検証
        assertNotNull(util);
    }

    public static class TestFormatterFactory extends FormatterFactory {
        @Override
        protected DataRecordFormatter createFormatter(String fileType,
                String formatFilePath) {
            if (fileType.equals("Hoge")) {
                return new HogeFormatter();
            } else if (fileType.equals("IoEx")) {
                    return new IOExFormatter();
            }
            return super.createFormatter(fileType, formatFilePath);
        }
    }

    public static class HogeFormatter implements DataRecordFormatter {
        @Override
        public DataRecord readRecord() throws IOException,
                InvalidDataFormatException {
            return null;
        }
        @Override
        public void writeRecord(Map<String, ?> record) throws IOException,
                InvalidDataFormatException {
        }
        @Override
        public void writeRecord(String recordType, Map<String, ?> record)
                throws IOException, InvalidDataFormatException {
        }
        @Override
        public DataRecordFormatter initialize() {
            return this;
        }
        @Override
        public DataRecordFormatter setInputStream(InputStream stream) {
            return this;
        }
        @Override
        public void close() {
        }

        @Override
        public DataRecordFormatter setDefinition(LayoutDefinition definition) {
            return this;
        }
        @Override
        public DataRecordFormatter setOutputStream(OutputStream stream) {
            return this;
        }
        @Override
        public boolean hasNext() throws IOException {
            return false;
        }

        @Override
        public int getRecordNumber() {
            return 0;
        }
    }

    public static class IOExFormatter extends HogeFormatter {
        @Override
        public DataRecord readRecord() throws IOException,
                InvalidDataFormatException {
            throw new IOException("DummyException");
        }
        @Override
        public void writeRecord(Map<String, ?> record) throws IOException,
                InvalidDataFormatException {
            throw new IOException("DummyException");
        }
    }
}
