package io.jobcopilot.resumeassistant.infrastructure.converter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/**
 * Word 文档转换器测试 / Word document converter tests
 */
class WordConverterTest {

    private WordConverter converter;
    private MockedStatic<ExternalCommandUtils> externalCommandUtilsMock;

    // 准备 / Given
    @BeforeEach
    @DisplayName("Should initialize Word converter before each test / 应在每次测试前初始化 Word 转换器")
    void setUp() {
        converter = new WordConverter();
        externalCommandUtilsMock = mockStatic(ExternalCommandUtils.class);
    }

    @AfterEach
    @DisplayName("Should clean up resources after each test / 应在每次测试后清理资源")
    void tearDown() {
        externalCommandUtilsMock.close();
    }

    @Test
    @DisplayName("Should convert DOCX to Markdown / 应将 DOCX 转换为 Markdown")
    void shouldConvertDocxToMd() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("test".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runPandoc(
                any(InputStream.class), eq("docx"), eq("md"), eq(null)
        )).thenReturn(expectedOutput);

        // 执行 / When
        InputStream result = converter.convert(input, "docx", "md");

        // 验证 / Then
        assertEquals(expectedOutput, result);
    }

    @Test
    @DisplayName("Should convert Markdown to DOCX / 应将 Markdown 转换为 DOCX")
    void shouldConvertMdToDocx() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("test".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runPandoc(
                any(InputStream.class), eq("md"), eq("docx"), eq(null)
        )).thenReturn(expectedOutput);

        // 执行 / When
        InputStream result = converter.convert(input, "md", "docx");

        // 验证 / Then
        assertEquals(expectedOutput, result);
    }

    @Test
    @DisplayName("Should convert DOCX to PDF / 应将 DOCX 转换为 PDF")
    void shouldConvertDocxToPdf() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("test".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runLibreOffice(
                any(InputStream.class), eq("docx"), eq("pdf")
        )).thenReturn(expectedOutput);

        // 执行 / When
        InputStream result = converter.convert(input, "docx", "pdf");

        // 验证 / Then
        assertEquals(expectedOutput, result);
    }

    @Test
    @DisplayName("Should throw exception for unsupported format / 应在不支持的格式时抛出异常")
    void shouldThrowExceptionForUnsupportedFormat() {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("test".getBytes());

        // 执行与验证 / When & Then
        assertThrows(IOException.class, () -> converter.convert(input, "txt", "md"));
    }
}
