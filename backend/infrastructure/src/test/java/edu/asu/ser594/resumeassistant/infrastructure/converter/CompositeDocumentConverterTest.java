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

class CompositeDocumentConverterTest {

    @Mock
    private DocumentFormatConverter mockConverter1;

    @Mock
    private DocumentFormatConverter mockConverter2;

    private CompositeDocumentConverter compositeConverter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        compositeConverter = new CompositeDocumentConverter(List.of(mockConverter1, mockConverter2));
    }

    @Test
    void shouldReturnSameStreamForSameFormat() throws IOException {
        byte[] content = "same format".getBytes();
        InputStream input = new ByteArrayInputStream(content);
        
        InputStream result = compositeConverter.convert(input, "md", "markdown");
        
        assertArrayEquals(content, result.readAllBytes());
    }

    @Test
    void shouldRouteToCorrectConverter() throws IOException {
        InputStream input = new ByteArrayInputStream("input".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        when(mockConverter1.supports("md", "pdf")).thenReturn(false);
        when(mockConverter2.supports("md", "pdf")).thenReturn(true);
        when(mockConverter2.convert(input, "md", "pdf")).thenReturn(expectedOutput);

        InputStream result = compositeConverter.convert(input, "md", "pdf");

        assertEquals(expectedOutput, result);
        verify(mockConverter1).supports("md", "pdf");
        verify(mockConverter2).convert(input, "md", "pdf");
    }

    @Test
    void shouldSkipSelfInConvertersList() throws IOException {
        InputStream input = new ByteArrayInputStream("input".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        when(mockConverter1.supports("md", "html")).thenReturn(true);
        when(mockConverter1.convert(input, "md", "html")).thenReturn(expectedOutput);

        CompositeDocumentConverter selfReferencingConverter = new CompositeDocumentConverter(
                List.of(compositeConverter, mockConverter1)
        );

        InputStream result = selfReferencingConverter.convert(input, "md", "html");

        assertEquals(expectedOutput, result);
        verify(mockConverter1).convert(input, "md", "html");
    }

    @Test
    void shouldThrowExceptionWhenNoConverterFound() {
        InputStream input = new ByteArrayInputStream("input".getBytes());

        when(mockConverter1.supports(anyString(), anyString())).thenReturn(false);
        when(mockConverter2.supports(anyString(), anyString())).thenReturn(false);

        assertThrows(IOException.class, () -> {
            compositeConverter.convert(input, "docx", "md");
        });
    }

    @Test
    void shouldReturnTrueIfAnyConverterSupports() {
        when(mockConverter1.supports("txt", "pdf")).thenReturn(false);
        when(mockConverter2.supports("txt", "pdf")).thenReturn(true);

        assertTrue(compositeConverter.supports("txt", "pdf"));
    }

    @Test
    void shouldReturnFalseIfNoConverterSupports() {
        when(mockConverter1.supports("txt", "docx")).thenReturn(false);
        when(mockConverter2.supports("txt", "docx")).thenReturn(false);

        assertFalse(compositeConverter.supports("txt", "docx"));
    }

    @Test
    void shouldGetAggregatedSupportedTargets() {
        when(mockConverter1.getSupportedTargets("md")).thenReturn(Arrays.asList("html", "txt"));
        when(mockConverter2.getSupportedTargets("md")).thenReturn(Arrays.asList("txt", "pdf"));

        List<String> targets = compositeConverter.getSupportedTargets("md");

        assertEquals(3, targets.size());
        assertTrue(targets.containsAll(Arrays.asList("html", "txt", "pdf")));
    }

    @Test
    void normalizeFormatHandlesEdgeCases() {
        assertFalse(compositeConverter.supports(null, "pdf"));
    }
}
