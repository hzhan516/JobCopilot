package io.jobcopilot.resumeassistant.infrastructure.converter;

import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 组合文档转换器测试 / Composite document converter tests
 */
class CompositeDocumentConverterTest {

    @Mock
    private DocumentFormatConverter mockConverter1;

    @Mock
    private DocumentFormatConverter mockConverter2;

    private CompositeDocumentConverter compositeConverter;

    // 准备 / Given
    @BeforeEach
    @DisplayName("Should initialize converter before each test / 应在每次测试前初始化转换器")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        compositeConverter = new CompositeDocumentConverter(List.of(mockConverter1, mockConverter2));
    }

    @Test
    @DisplayName("Should return same stream for same format / 应在相同格式时返回相同流")
    void shouldReturnSameStreamForSameFormat() throws IOException {
        // 准备 / Given
        byte[] content = "same format".getBytes();
        InputStream input = new ByteArrayInputStream(content);

        // 执行 / When
        InputStream result = compositeConverter.convert(input, "md", "markdown");

        // 验证 / Then
        assertArrayEquals(content, result.readAllBytes());
    }

    @Test
    @DisplayName("Should route to correct converter / 应路由到正确的转换器")
    void shouldRouteToCorrectConverter() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("input".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        when(mockConverter1.supports("md", "pdf")).thenReturn(false);
        when(mockConverter2.supports("md", "pdf")).thenReturn(true);
        when(mockConverter2.convert(input, "md", "pdf")).thenReturn(expectedOutput);

        // 执行 / When
        InputStream result = compositeConverter.convert(input, "md", "pdf");

        // 验证 / Then
        assertEquals(expectedOutput, result);
        verify(mockConverter1).supports("md", "pdf");
        verify(mockConverter2).convert(input, "md", "pdf");
    }

    @Test
    @DisplayName("Should skip self in converters list / 应在转换器列表中跳过自身")
    void shouldSkipSelfInConvertersList() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("input".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        when(mockConverter1.supports("md", "html")).thenReturn(true);
        when(mockConverter1.convert(input, "md", "html")).thenReturn(expectedOutput);

        CompositeDocumentConverter selfReferencingConverter = new CompositeDocumentConverter(
                List.of(compositeConverter, mockConverter1)
        );

        // 执行 / When
        InputStream result = selfReferencingConverter.convert(input, "md", "html");

        // 验证 / Then
        assertEquals(expectedOutput, result);
        verify(mockConverter1).convert(input, "md", "html");
    }

    @Test
    @DisplayName("Should throw exception when no converter found / 应在找不到转换器时抛出异常")
    void shouldThrowExceptionWhenNoConverterFound() {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("input".getBytes());

        when(mockConverter1.supports(anyString(), anyString())).thenReturn(false);
        when(mockConverter2.supports(anyString(), anyString())).thenReturn(false);

        // 执行与验证 / When & Then
        assertThrows(IOException.class, () -> compositeConverter.convert(input, "docx", "md"));
    }

    @Test
    @DisplayName("Should return true if any converter supports / 应在任意转换器支持时返回 true")
    void shouldReturnTrueIfAnyConverterSupports() {
        // 准备 / Given
        when(mockConverter1.supports("txt", "pdf")).thenReturn(false);
        when(mockConverter2.supports("txt", "pdf")).thenReturn(true);

        // 执行与验证 / When & Then
        assertTrue(compositeConverter.supports("txt", "pdf"));
    }

    @Test
    @DisplayName("Should return false if no converter supports / 应在无转换器支持时返回 false")
    void shouldReturnFalseIfNoConverterSupports() {
        // 准备 / Given
        when(mockConverter1.supports("txt", "docx")).thenReturn(false);
        when(mockConverter2.supports("txt", "docx")).thenReturn(false);

        // 执行与验证 / When & Then
        assertFalse(compositeConverter.supports("txt", "docx"));
    }

    @Test
    @DisplayName("Should get aggregated supported targets / 应获取聚合的支持目标格式")
    void shouldGetAggregatedSupportedTargets() {
        // 准备 / Given
        when(mockConverter1.getSupportedTargets("md")).thenReturn(Arrays.asList("html", "txt"));
        when(mockConverter2.getSupportedTargets("md")).thenReturn(Arrays.asList("txt", "pdf"));

        // 执行 / When
        List<String> targets = compositeConverter.getSupportedTargets("md");

        // 验证 / Then
        assertEquals(3, targets.size());
        assertTrue(targets.containsAll(Arrays.asList("html", "txt", "pdf")));
    }

    @Test
    @DisplayName("Should handle edge cases when normalizing format / 应在格式化时处理边界情况")
    void normalizeFormatHandlesEdgeCases() {
        // 执行与验证 / When & Then
        assertFalse(compositeConverter.supports(null, "pdf"));
    }

    @Test
    @DisplayName("Should support chain conversion / 应支持链式转换")
    void shouldSupportChainConversion() {
        // 准备 / Given
        when(mockConverter1.supports("docx", "md")).thenReturn(true);
        when(mockConverter1.supports("md", "html")).thenReturn(false);
        when(mockConverter2.supports("docx", "md")).thenReturn(false);
        when(mockConverter2.supports("md", "html")).thenReturn(true);

        // 执行与验证 / When & Then
        assertTrue(compositeConverter.supports("docx", "html"));
    }

    @Test
    @DisplayName("Should perform chain conversion / 应执行链式转换")
    void shouldPerformChainConversion() throws IOException {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("docx content".getBytes());
        InputStream intermediate = new ByteArrayInputStream("md content".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("html content".getBytes());

        when(mockConverter1.supports("docx", "md")).thenReturn(true);
        when(mockConverter2.supports("md", "html")).thenReturn(true);
        when(mockConverter1.convert(any(), eq("docx"), eq("md"))).thenReturn(intermediate);
        when(mockConverter2.convert(any(), eq("md"), eq("html"))).thenReturn(expectedOutput);

        // 执行 / When
        InputStream result = compositeConverter.convert(input, "docx", "html");

        // 验证 / Then
        assertEquals(expectedOutput, result);
    }

    @Test
    @DisplayName("Should fallback to pure text converter on failure / 应在失败时回退到纯文本转换器")
    void shouldFallbackToPureTextConverterOnFailure() throws IOException {
        // 准备 / Given — 模拟一个失败的外部命令转换器和一个成功的 MarkdownConverter
        DocumentFormatConverter failingConverter = mock(DocumentFormatConverter.class);
        DocumentFormatConverter markdownConverter = new MarkdownConverter();

        CompositeDocumentConverter converterWithFallback = new CompositeDocumentConverter(
                List.of(failingConverter, markdownConverter)
        );

        InputStream input = new ByteArrayInputStream("# Hello".getBytes());

        when(failingConverter.supports("md", "html")).thenReturn(true);
        when(failingConverter.convert(any(), eq("md"), eq("html")))
                .thenThrow(new IOException("Pandoc failed"));

        // 执行 / When
        InputStream result = converterWithFallback.convert(input, "md", "html");

        // 验证 / Then
        assertNotNull(result);
        String resultStr = new String(result.readAllBytes());
        assertTrue(resultStr.contains("<h1>Hello</h1>") || resultStr.contains("Hello"));
    }
}
