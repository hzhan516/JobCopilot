package edu.asu.ser594.resumeassistant.infrastructure.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Markdown 转换器测试 / Markdown converter tests
 */
class MarkdownConverterTest {

    private MarkdownConverter converter;

    // 准备 / Given
    @BeforeEach
    void setUp() {
        converter = new MarkdownConverter();
    }

    @Test
    void shouldReturnSameStreamForSameFormat() throws IOException {
        // 准备 / Given
        String content = "# Title";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        // 执行 / When
        InputStream result = converter.convert(input, "md", "md");
        String resultString = new String(result.readAllBytes());

        // 验证 / Then
        assertEquals(content, resultString);
    }

    @Test
    void shouldConvertMdToHtml() throws IOException {
        // 准备 / Given
        String content = "**Bold** and *Italic*";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        // 执行 / When
        InputStream result = converter.convert(input, "md", "html");
        String resultString = new String(result.readAllBytes());

        // 验证 / Then
        assertTrue(resultString.contains("<strong>Bold</strong>"));
        assertTrue(resultString.contains("<em>Italic</em>"));
    }

    @Test
    void shouldConvertMdToTxt() throws IOException {
        // 准备 / Given
        String content = "# Title\n\nSome **bold** text.";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        // 执行 / When
        InputStream result = converter.convert(input, "md", "txt");
        String resultString = new String(result.readAllBytes());

        // 验证 / Then
        assertFalse(resultString.contains("<h1>"));
        assertFalse(resultString.contains("<strong>"));
        assertTrue(resultString.contains("Title"));
        assertTrue(resultString.contains("Some bold text."));
    }

    @Test
    void shouldConvertHtmlToMd() throws IOException {
        // 准备 / Given
        String content = "<p><strong>Bold</strong> text</p>";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        // 执行 / When
        InputStream result = converter.convert(input, "html", "md");
        String resultString = new String(result.readAllBytes());

        // 验证 / Then
        assertTrue(resultString.contains("**Bold** text"));
        assertFalse(resultString.contains("<p>"));
    }

    @Test
    void shouldConvertHtmlToTxt() throws IOException {
        // 准备 / Given
        String content = "<p><strong>Bold</strong> text<br/>new line</p>";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        // 执行 / When
        InputStream result = converter.convert(input, "html", "txt");
        String resultString = new String(result.readAllBytes());

        // 验证 / Then
        assertFalse(resultString.contains("<strong>"));
        assertFalse(resultString.contains("<br/>"));
        assertTrue(resultString.contains("Bold text\nnew line"));
    }

    @Test
    void shouldConvertTxtToMd() throws IOException {
        // 准备 / Given
        String content = "Plain text content";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        // 执行 / When
        InputStream result = converter.convert(input, "txt", "md");
        String resultString = new String(result.readAllBytes());

        // 验证 / Then
        assertEquals("```\nPlain text content\n```", resultString);
    }

    @Test
    void shouldConvertTxtToHtml() throws IOException {
        // 准备 / Given
        String content = "Text with < tag";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        // 执行 / When
        InputStream result = converter.convert(input, "txt", "html");
        String resultString = new String(result.readAllBytes());

        // 验证 / Then
        assertEquals("<pre>Text with &lt; tag</pre>", resultString);
    }

    @Test
    void shouldThrowExceptionForUnsupportedFormat() {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("content".getBytes());

        // 执行与验证 / When & Then
        assertThrows(IOException.class, () -> {
            converter.convert(input, "md", "docx");
        });
    }
}
