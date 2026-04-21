package edu.asu.ser594.resumeassistant.infrastructure.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractDocumentConverterTest {

    private TestDocumentConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TestDocumentConverter();
    }

    @Test
    void shouldRegisterAndSupportFormats() {
        assertTrue(converter.supports("md", "html"));
        assertTrue(converter.supports("md", "txt"));
        assertTrue(converter.supports("MD", "HTML"));
        
        assertFalse(converter.supports("md", "pdf"));
        assertFalse(converter.supports("html", "md"));
        assertFalse(converter.supports(null, "html"));
        assertFalse(converter.supports("md", null));
    }

    @Test
    void shouldGetSupportedTargets() {
        List<String> targets = converter.getSupportedTargets("md");
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
        String originalContent = "Hello, World!";
        InputStream stream = converter.testToStream(originalContent);
        
        assertNotNull(stream);
        
        String result = converter.testToString(stream);
        assertEquals(originalContent, result);
    }

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
