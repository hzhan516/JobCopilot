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

/** Word 文档转换器测试 / Word document converter tests */
class WordConverterTest {

    private WordConverter converter;
    private MockedStatic<ExternalCommandUtils> externalCommandUtilsMock;

    // 准备 / Given
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
    void shouldThrowExceptionForUnsupportedFormat() {
        // 准备 / Given
        InputStream input = new ByteArrayInputStream("test".getBytes());

        // 执行与验证 / When & Then
        assertThrows(IOException.class, () -> {
            converter.convert(input, "txt", "docx");
        });
    }
}
