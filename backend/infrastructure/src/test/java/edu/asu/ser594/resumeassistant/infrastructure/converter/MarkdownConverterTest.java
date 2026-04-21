package edu.asu.ser594.resumeassistant.infrastructure.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownConverterTest {

    private MarkdownConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MarkdownConverter();
    }

    @Test
    void shouldReturnSameStreamForSameFormat() throws IOException {
        String content = "# Title";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        InputStream result = converter.convert(input, "md", "md");
        String resultString = new String(result.readAllBytes());
        
        assertEquals(content, resultString);
    }

    @Test
    void shouldConvertMdToHtml() throws IOException {
        String content = "**Bold** and *Italic*";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        InputStream result = converter.convert(input, "md", "html");
        String resultString = new String(result.readAllBytes());
        
        assertTrue(resultString.contains("<strong>Bold</strong>"));
        assertTrue(resultString.contains("<em>Italic</em>"));
    }

    @Test
    void shouldConvertMdToTxt() throws IOException {
        String content = "# Title\n\nSome **bold** text.";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        InputStream result = converter.convert(input, "md", "txt");
        String resultString = new String(result.readAllBytes());
        
        assertFalse(resultString.contains("<h1>"));
        assertFalse(resultString.contains("<strong>"));
        assertTrue(resultString.contains("Title"));
        assertTrue(resultString.contains("Some bold text."));
    }

    @Test
    void shouldConvertHtmlToMd() throws IOException {
        String content = "<p><strong>Bold</strong> text</p>";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        InputStream result = converter.convert(input, "html", "md");
        String resultString = new String(result.readAllBytes());
        
        assertTrue(resultString.contains("**Bold** text"));
        assertFalse(resultString.contains("<p>"));
    }

    @Test
    void shouldConvertHtmlToTxt() throws IOException {
        String content = "<p><strong>Bold</strong> text<br/>new line</p>";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        InputStream result = converter.convert(input, "html", "txt");
        String resultString = new String(result.readAllBytes());
        
        assertFalse(resultString.contains("<strong>"));
        assertFalse(resultString.contains("<br/>"));
        assertTrue(resultString.contains("Bold text\nnew line"));
    }

    @Test
    void shouldConvertTxtToMd() throws IOException {
        String content = "Plain text content";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        InputStream result = converter.convert(input, "txt", "md");
        String resultString = new String(result.readAllBytes());
        
        assertEquals("```\nPlain text content\n```", resultString);
    }

    @Test
    void shouldConvertTxtToHtml() throws IOException {
        String content = "Text with < tag";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        InputStream result = converter.convert(input, "txt", "html");
        String resultString = new String(result.readAllBytes());
        
        assertEquals("<pre>Text with &lt; tag</pre>", resultString);
    }

    @Test
    void shouldThrowExceptionForUnsupportedFormat() {
        InputStream input = new ByteArrayInputStream("content".getBytes());
        
        assertThrows(IOException.class, () -> {
            converter.convert(input, "md", "docx");
        });
    }
}
