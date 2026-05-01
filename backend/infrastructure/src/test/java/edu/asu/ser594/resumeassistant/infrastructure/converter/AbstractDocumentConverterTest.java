package edu.asu.ser594.resumeassistant.infrastructure.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 抽象文档转换器测试 / Abstract document converter tests */
class AbstractDocumentConverterTest {

    private TestDocumentConverter converter;

    // 准备 / Given
    @BeforeEach
    void setUp() {
        converter = new TestDocumentConverter();
    }

    @Test
    void shouldRegisterAndSupportFormats() {
        // 验证支持的格式 / Verify supported formats
        assertTrue(converter.supports("md", "html"));
        assertTrue(converter.supports("md", "txt"));
        assertTrue(converter.supports("MD", "HTML"));
        
        // 验证不支持的格式 / Verify unsupported formats
        assertFalse(converter.supports("md", "pdf"));
        assertFalse(converter.supports("html", "md"));
        assertFalse(converter.supports(null, "html"));
        assertFalse(converter.supports("md", null));
    }

    @Test
    void shouldGetSupportedTargets() {
        // 执行 / When
        List<String> targets = converter.getSupportedTargets("md");

        // 验证 / Then
        assertNotNull(targets);
        assertEquals(2, targets.size());
        assertTrue(targets.contains("html"));
        assertTrue(targets.contains("txt"));

        List<String> upperTargets = converter.getSupportedTargets("MD");
        assertEquals(2, upperTargets.size());

        assertTrue(converter.getSupportedTargets("unsupported").isEmpty());
        assertTrue(converter.getSupportedTargets(null).isEmpty());
    }

    @Test
    void shouldConvertStringToStreamAndBack() throws IOException {
        // 准备 / Given
        String originalContent = "Hello, World!";

        // 执行 / When
        InputStream stream = converter.testToStream(originalContent);
        assertNotNull(stream);
        
        String result = converter.testToString(stream);

        // 验证 / Then
        assertEquals(originalContent, result);
    }

    // 测试用文档转换器 / Test document converter
    private static class TestDocumentConverter extends AbstractDocumentConverter {
        public TestDocumentConverter() {
            register("md", "html", "txt");
        }

        @Override
        public InputStream convert(InputStream source, String sourceFormat, String targetFormat) throws IOException {
            return null;
        }

        public InputStream testToStream(String content) {
            return super.toStream(content);
        }

        public String testToString(InputStream stream) throws IOException {
            return super.toString(stream);
        }
    }
}
