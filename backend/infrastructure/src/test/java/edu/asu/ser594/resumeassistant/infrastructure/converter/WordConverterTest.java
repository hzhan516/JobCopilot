package edu.asu.ser594.resumeassistant.infrastructure.converter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class WordConverterTest {

    private WordConverter converter;
    private MockedStatic<ExternalCommandUtils> externalCommandUtilsMock;

    @BeforeEach
    void setUp() {
        converter = new WordConverter();
        externalCommandUtilsMock = mockStatic(ExternalCommandUtils.class);
    }

    @AfterEach
    void tearDown() {
        externalCommandUtilsMock.close();
    }

    @Test
    void shouldConvertDocxToMd() throws IOException {
        InputStream input = new ByteArrayInputStream("test".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runPandoc(
                any(InputStream.class), eq("docx"), eq("md"), eq(null)
        )).thenReturn(expectedOutput);

        InputStream result = converter.convert(input, "docx", "md");
        
        assertEquals(expectedOutput, result);
    }

    @Test
    void shouldConvertMdToDocx() throws IOException {
        InputStream input = new ByteArrayInputStream("test".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runPandoc(
                any(InputStream.class), eq("md"), eq("docx"), eq(null)
        )).thenReturn(expectedOutput);

        InputStream result = converter.convert(input, "md", "docx");
        
        assertEquals(expectedOutput, result);
    }

    @Test
    void shouldConvertDocxToPdf() throws IOException {
        InputStream input = new ByteArrayInputStream("test".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("output".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runLibreOffice(
                any(InputStream.class), eq("docx"), eq("pdf")
        )).thenReturn(expectedOutput);

        InputStream result = converter.convert(input, "docx", "pdf");
        
        assertEquals(expectedOutput, result);
    }

    @Test
    void shouldThrowExceptionForUnsupportedFormat() {
        InputStream input = new ByteArrayInputStream("test".getBytes());
        
        assertThrows(IOException.class, () -> {
            converter.convert(input, "txt", "docx");
        });
    }
}
