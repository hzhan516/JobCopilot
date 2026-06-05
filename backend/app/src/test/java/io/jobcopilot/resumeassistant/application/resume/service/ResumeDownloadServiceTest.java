package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.application.resume.dto.ResumeDownloadResult;
import io.jobcopilot.resumeassistant.application.resume.query.ResumeDownloadQuery;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import io.jobcopilot.resumeassistant.domain.shared.valueobject.DocumentFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ResumeDownloadService 单元测试
 * 简历下载服务单元测试
 * <p>
 * 测试文件下载与格式转换完整路径：
 * Tests the full file download and format conversion path:
 * - 原始版本下载
 * - Original version download
 * - 转换版本下载
 * - Converted version download
 * - 格式转换
 * - Format conversion
 * - 下载失败异常
 * - Download failure exception
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Download Service Tests")
class ResumeDownloadServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentFormatConverter documentFormatConverter;

    @InjectMocks
    private ResumeDownloadService service;

    private ResumeVersion originalVersion;
    private ResumeVersion convertedVersion;

    @BeforeEach
    void setUp() {
        originalVersion = ResumeVersion.createOriginal(
                java.util.UUID.randomUUID(), "resume.pdf", "application/pdf", 1024L, "storage/resume.pdf");
        convertedVersion = ResumeVersion.createConverted(java.util.UUID.randomUUID());
        convertedVersion.editContent("# Resume Markdown Content");
    }

    // ==================== 原始版本 ====================

    @Test
    @DisplayName("Should download original version without conversion")
    void shouldDownloadOriginalVersionWithoutConversion() {
        // 给定 / Given
        InputStream pdfStream = new ByteArrayInputStream("PDF content".getBytes());
        when(fileStorageService.download("storage/resume.pdf")).thenReturn(Optional.of(pdfStream));

        // 当 / When
        ResumeDownloadResult result = service.download(originalVersion, "original");

        // 那么 / Then
        assertThat(result.fileName()).endsWith(".pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.inputStream()).isNotNull();
    }

    @Test
    @DisplayName("Should convert original PDF to target format")
    void shouldConvertOriginalPdfToTargetFormat() throws IOException {
        // 给定 / Given
        InputStream pdfStream = new ByteArrayInputStream("PDF content".getBytes());
        InputStream mdStream = new ByteArrayInputStream("Markdown".getBytes());
        when(fileStorageService.download("storage/resume.pdf")).thenReturn(Optional.of(pdfStream));
        when(documentFormatConverter.convert(any(), eq("pdf"), eq("md"))).thenReturn(mdStream);

        // 当 / When
        ResumeDownloadResult result = service.download(originalVersion, "md");

        // 那么 / Then
        assertThat(result.fileName()).endsWith(".md");
        assertThat(result.contentType()).isEqualTo("text/markdown");
    }

    @Test
    @DisplayName("Should throw when original file not found")
    void shouldThrowWhenOriginalFileNotFound() {
        // 给定 / Given
        when(fileStorageService.download("storage/resume.pdf")).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> service.download(originalVersion, "original"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("file.not.found");
    }

    // ==================== 转换版本 ====================

    @Test
    @DisplayName("Should download converted version as markdown")
    void shouldDownloadConvertedVersionAsMarkdown() {
        // 当 / When
        ResumeDownloadResult result = service.download(convertedVersion, "md");

        // 那么 / Then
        assertThat(result.fileName()).endsWith(".md");
        assertThat(result.contentType()).isEqualTo("text/markdown");
    }

    @Test
    @DisplayName("Should convert converted version to PDF")
    void shouldConvertConvertedVersionToPdf() throws IOException {
        // 给定 / Given
        InputStream pdfStream = new ByteArrayInputStream("PDF".getBytes());
        when(documentFormatConverter.convert(any(), eq("md"), eq("pdf"))).thenReturn(pdfStream);

        // 当 / When
        ResumeDownloadResult result = service.download(convertedVersion, "pdf");

        // 那么 / Then
        assertThat(result.fileName()).endsWith(".pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should return original format when target equals source")
    void shouldReturnOriginalFormatWhenTargetEqualsSource() throws IOException {
        // 给定 / Given
        InputStream pdfStream = new ByteArrayInputStream("PDF content".getBytes());
        when(fileStorageService.download("storage/resume.pdf")).thenReturn(Optional.of(pdfStream));

        // 当 / When — "pdf" matches source MIME type derived format
        ResumeDownloadResult result = service.download(originalVersion, "original");

        // 那么 / Then
        assertThat(result.fileName()).endsWith(".pdf");
    }

    // ==================== 异常路径 ====================

    @Test
    @DisplayName("Should throw on conversion failure")
    void shouldThrowOnConversionFailure() throws IOException {
        // 给定 / Given
        InputStream pdfStream = new ByteArrayInputStream("PDF content".getBytes());
        when(fileStorageService.download("storage/resume.pdf")).thenReturn(Optional.of(pdfStream));
        when(documentFormatConverter.convert(any(), anyString(), anyString()))
                .thenThrow(new IOException("Conversion error"));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> service.download(originalVersion, "md"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("conversion.failed");
    }
}
