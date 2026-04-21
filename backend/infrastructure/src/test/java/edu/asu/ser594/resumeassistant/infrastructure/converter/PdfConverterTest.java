package edu.asu.ser594.resumeassistant.infrastructure.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PdfConverterTest {

    private PdfConverter converter;
    private MockedStatic<ExternalCommandUtils> externalCommandUtilsMock;

    @BeforeEach
    void setUp() {
        converter = new PdfConverter();
        externalCommandUtilsMock = mockStatic(ExternalCommandUtils.class);
    }

    @AfterEach
    void tearDown() {
        externalCommandUtilsMock.close();
    }

    @Test
    void shouldReturnSameStreamForSameFormat() throws IOException {
        byte[] content = "dummy pdf".getBytes();
        InputStream input = new ByteArrayInputStream(content);
        
        InputStream result = converter.convert(input, "pdf", "pdf");
        
        assertArrayEquals(content, result.readAllBytes());
    }

    @Test
    void shouldConvertMdToPdfUsingPandoc() throws IOException {
        InputStream input = new ByteArrayInputStream("md content".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("pdf content".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runPandoc(
                any(InputStream.class), eq("md"), eq("pdf"), anyString()
        )).thenReturn(expectedOutput);

        InputStream result = converter.convert(input, "md", "pdf");
        
        assertEquals(expectedOutput, result);
    }

    @Test
    void shouldConvertHtmlToPdfUsingPandoc() throws IOException {
        InputStream input = new ByteArrayInputStream("html content".getBytes());
        InputStream expectedOutput = new ByteArrayInputStream("pdf content".getBytes());

        externalCommandUtilsMock.when(() -> ExternalCommandUtils.runPandoc(
                any(InputStream.class), eq("html"), eq("pdf"), anyString()
        )).thenReturn(expectedOutput);

        InputStream result = converter.convert(input, "html", "pdf");
        
        assertEquals(expectedOutput, result);
    }

    @Test
    void shouldConvertPdfToTxtUsingPdfBox() throws IOException {
        InputStream input = new ByteArrayInputStream("dummy pdf".getBytes());
        PDDocument mockedDocument = mock(PDDocument.class);

        try (MockedStatic<Loader> loaderMock = mockStatic(Loader.class);
             MockedConstruction<PDFTextStripper> stripperConstruction = mockConstruction(PDFTextStripper.class, 
                     (mock, context) -> when(mock.getText(mockedDocument)).thenReturn("Extracted Text"))) {
            
            loaderMock.when(() -> Loader.loadPDF(any(byte[].class))).thenReturn(mockedDocument);

            InputStream result = converter.convert(input, "pdf", "txt");
            String resultText = new String(result.readAllBytes());

            assertEquals("Extracted Text", resultText);
        }
    }

    @Test
    void shouldThrowExceptionForUnsupportedFormat() {
        InputStream input = new ByteArrayInputStream("content".getBytes());
        
        assertThrows(IOException.class, () -> {
            converter.convert(input, "docx", "pdf");
        });
    }
}
