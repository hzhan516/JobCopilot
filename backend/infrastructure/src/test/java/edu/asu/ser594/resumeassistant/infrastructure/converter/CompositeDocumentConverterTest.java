package edu.asu.ser594.resumeassistant.infrastructure.converter;

import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 组合文档转换器测试 / Composite document converter tests */
class CompositeDocumentConverterTest {

    @Mock
    private DocumentFormatConverter mockConverter1;

    @Mock
    private DocumentFormatConverter mockConverter2;

    private CompositeDocumentConverter compositeConverter;

    // 准备 / Given
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        compositeConverter = new CompositeDocumentConverter(List.of(mockConverter1, mockConverter2));
    }

    @Test
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
    void shouldThrowExceptionWhenNoConverterFound() {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("input".getBytes());

        when(mockConverter1.supports(anyString(), anyString())).thenReturn(false);
        when(mockConverter2.supports(anyString(), anyString())).thenReturn(false);

        // 执行与验证 / When & Then
        assertThrows(IOException.class, () -> {
            compositeConverter.convert(input, "docx", "md");
        });
    }

    @Test
    void shouldReturnTrueIfAnyConverterSupports() {
        // 准备 / Given
        when(mockConverter1.supports("txt", "pdf")).thenReturn(false);
        when(mockConverter2.supports("txt", "pdf")).thenReturn(true);

        // 执行与验证 / When & Then
        assertTrue(compositeConverter.supports("txt", "pdf"));
    }

    @Test
    void shouldReturnFalseIfNoConverterSupports() {
        // 准备 / Given
        when(mockConverter1.supports("txt", "docx")).thenReturn(false);
        when(mockConverter2.supports("txt", "docx")).thenReturn(false);

        // 执行与验证 / When & Then
        assertFalse(compositeConverter.supports("txt", "docx"));
    }

    @Test
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
    void normalizeFormatHandlesEdgeCases() {
        // 执行与验证 / When & Then
        assertFalse(compositeConverter.supports(null, "pdf"));
    }
}
