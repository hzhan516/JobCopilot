package io.jobcopilot.resumeassistant.infrastructure.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PDF 文档转换器测试 / PDF document converter tests
 */
class PdfConverterTest {

    private PdfConverter converter;
    private MockedStatic<ExternalCommandUtils> externalCommandUtilsMock;

    // 准备 / Given
    @BeforeEach
    @DisplayName("Should initialize PDF converter before each test / 应在每次测试前初始化 PDF 转换器")
    void setUp() {
        converter = new PdfConverter();
        externalCommandUtilsMock = mockStatic(ExternalCommandUtils.class);
    }

    @AfterEach
    @DisplayName("Should clean up resources after each test / 应在每次测试后清理资源")
    void tearDown() {
        externalCommandUtilsMock.close();
    }

    @Test
    @DisplayName("Should return same stream for same format / 应在相同格式时返回相同流")
    void shouldReturnSameStreamForSameFormat() throws IOException {
        // 准备 / Given
        byte[] content = "dummy pdf".getBytes();
        InputStream input = new ByteArrayInputStream(content);

        // 执行 / When
        InputStream result = converter.convert(input, "pdf", "pdf");

        // 验证 / Then
        assertArrayEquals(content, result.readAllBytes());
    }

    @Test
    @DisplayName("Should convert Markdown to PDF using Pandoc / 应使用 Pandoc 将 Markdown 转换为 PDF")
    void shouldConvertMdToPdfUsingPandoc() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("md content".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("pdf content".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runPandoc(
                any(InputStream.class), eq("md"), eq("pdf"), anyString()
        )).thenReturn(expectedOutput);

        // 执行 / When
        InputStream result = converter.convert(input, "md", "pdf");

        // 验证 / Then
        assertEquals(expectedOutput, result);
    }

    @Test
    @DisplayName("Should convert HTML to PDF using Pandoc / 应使用 Pandoc 将 HTML 转换为 PDF")
    void shouldConvertHtmlToPdfUsingPandoc() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("html content".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("pdf content".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runPandoc(
                any(InputStream.class), eq("html"), eq("pdf"), anyString()
        )).thenReturn(expectedOutput);

        // 执行 / When
        InputStream result = converter.convert(input, "html", "pdf");

        // 验证 / Then
        assertEquals(expectedOutput, result);
    }

    @Test
    @DisplayName("Should convert PDF to text using PDFBox / 应使用 PDFBox 将 PDF 转换为文本")
    void shouldConvertPdfToTxtUsingPdfBox() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("dummy pdf".getBytes());
        PDDocument mockedDocument = mock(PDDocument.class);

        // 执行 / When
        try (MockedStatic<Loader> loaderMock = mockStatic(Loader.class);
             MockedConstruction<PDFTextStripper> stripperConstruction = mockConstruction(PDFTextStripper.class,
                     (mock, context) -> when(mock.getText(mockedDocument)).thenReturn("Extracted Text"))) {

            loaderMock.when(() -> Loader.loadPDF(any(byte[].class))).thenReturn(mockedDocument);

            InputStream result = converter.convert(input, "pdf", "txt");
            String resultText = new String(result.readAllBytes());

            // 验证 / Then
            assertEquals("Extracted Text", resultText);
        }
    }

    @Test
    @DisplayName("Should throw exception for unsupported format / 应在不支持的格式时抛出异常")
    void shouldThrowExceptionForUnsupportedFormat() {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("content".getBytes());

        // 执行与验证 / When & Then
        assertThrows(IOException.class, () -> {
            converter.convert(input, "docx", "pdf");
        });
    }
}
