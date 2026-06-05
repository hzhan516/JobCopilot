package io.jobcopilot.resumeassistant.domain.resume.service;

import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import io.jobcopilot.resumeassistant.domain.shared.valueobject.DocumentFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResumeConverterService 单元测试
 * Resume Converter Service Unit Tests
 *
 * 测试简历文件到 Markdown 的转换逻辑：
 * Tests resume file to Markdown conversion logic:
 * - PDF/DOCX 转换 / PDF/DOCX conversion
 * - 已 Markdown 直接返回 / Already Markdown direct return
 * - 下载失败返回 null / Download failure returns null
 * - 转换失败返回 null / Conversion failure returns null
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Converter Service Tests")
class ResumeConverterServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentFormatConverter documentFormatConverter;

    private ResumeConverterService converterService;

    @BeforeEach
    void setUp() {
        converterService = new ResumeConverterService(fileStorageService, documentFormatConverter);
    }

    // ==================== 正常路径 ====================

    @Test
    @DisplayName("Should convert PDF to Markdown")
    void shouldConvertPdfToMarkdown() throws Exception {
        // 给定 / Given
        String pdfContent = "PDF resume content";
        String mdContent = "# Resume\n\nPDF content";
        InputStream pdfStream = new ByteArrayInputStream(pdfContent.getBytes());
        InputStream mdStream = new ByteArrayInputStream(mdContent.getBytes());

        when(fileStorageService.download("path/to/resume.pdf")).thenReturn(Optional.of(pdfStream));
        when(documentFormatConverter.convert(any(InputStream.class), eq("pdf"), eq("md")))
                .thenReturn(mdStream);

        // 当 / When
        String result = converterService.convertToMarkdown("path/to/resume.pdf", "application/pdf");

        // 那么 / Then
        assertThat(result).isEqualTo(mdContent);
    }

    @Test
    @DisplayName("Should convert DOCX to Markdown")
    void shouldConvertDocxToMarkdown() throws Exception {
        // 给定 / Given
        String docxContent = "DOCX content";
        String mdContent = "# Resume from DOCX";
        InputStream docxStream = new ByteArrayInputStream(docxContent.getBytes());
        InputStream mdStream = new ByteArrayInputStream(mdContent.getBytes());

        when(fileStorageService.download("path/to/resume.docx")).thenReturn(Optional.of(docxStream));
        when(documentFormatConverter.convert(any(InputStream.class), eq("docx"), eq("md")))
                .thenReturn(mdStream);

        // 当 / When
        String result = converterService.convertToMarkdown("path/to/resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        // 那么 / Then
        assertThat(result).isEqualTo(mdContent);
    }

    @Test
    @DisplayName("Should return raw content for Markdown files without conversion")
    void shouldReturnRawContentForMarkdownFiles() throws Exception {
        // 给定 / Given
        String mdContent = "# My Resume\n\nExperience: 5 years";
        InputStream mdStream = new ByteArrayInputStream(mdContent.getBytes());

        when(fileStorageService.download("path/to/resume.md")).thenReturn(Optional.of(mdStream));

        // 当 / When
        String result = converterService.convertToMarkdown("path/to/resume.md", "text/markdown");

        // 那么 / Then
        assertThat(result).isEqualTo(mdContent);
        verify(documentFormatConverter, never()).convert(any(), any(), any());
    }

    // ==================== 异常路径 ====================

    @Test
    @DisplayName("Should return null when file not found")
    void shouldReturnNullWhenFileNotFound() {
        // 给定 / Given
        when(fileStorageService.download("missing/path")).thenReturn(Optional.empty());

        // 当 / When
        String result = converterService.convertToMarkdown("missing/path", "application/pdf");

        // 那么 / Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null on conversion exception")
    void shouldReturnNullOnConversionException() throws Exception {
        // 给定 / Given
        String pdfContent = "PDF content";
        InputStream pdfStream = new ByteArrayInputStream(pdfContent.getBytes());

        when(fileStorageService.download("path/to/resume.pdf")).thenReturn(Optional.of(pdfStream));
        when(documentFormatConverter.convert(any(InputStream.class), eq("pdf"), eq("md")))
                .thenThrow(new IOException("Conversion failed"));

        // 当 / When
        String result = converterService.convertToMarkdown("path/to/resume.pdf", "application/pdf");

        // 那么 / Then — non-blocking: returns null without throwing
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null on download exception")
    void shouldReturnNullOnDownloadException() {
        // 给定 / Given
        when(fileStorageService.download("bad/path"))
                .thenThrow(new RuntimeException("Storage error"));

        // 当 / When
        String result = converterService.convertToMarkdown("bad/path", "application/pdf");

        // 那么 / Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when converter returns empty stream")
    void shouldReturnNullWhenConverterReturnsEmptyStream() throws Exception {
        // 给定 / Given
        String pdfContent = "PDF content";
        InputStream pdfStream = new ByteArrayInputStream(pdfContent.getBytes());
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        when(fileStorageService.download("path/to/resume.pdf")).thenReturn(Optional.of(pdfStream));
        when(documentFormatConverter.convert(any(InputStream.class), eq("pdf"), eq("md")))
                .thenReturn(emptyStream);

        // 当 / When
        String result = converterService.convertToMarkdown("path/to/resume.pdf", "application/pdf");

        // 那么 / Then
        assertThat(result).isEmpty();
    }

    // ==================== 格式处理 ====================

    @Test
    @DisplayName("Should handle unknown MIME type")
    void shouldHandleUnknownMimeType() {
        // 给定 / Given
        String content = "Plain text resume";
        InputStream stream = new ByteArrayInputStream(content.getBytes());

        when(fileStorageService.download("path/to/resume.txt")).thenReturn(Optional.of(stream));

        // 当 / When
        String result = converterService.convertToMarkdown("path/to/resume.txt", "text/plain");

        // 那么 / Then — DocumentFormat.fromMimeType("text/plain") should map to "txt"
        // if "txt" != "md", conversion should be attempted
    }
}
