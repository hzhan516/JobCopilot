package edu.asu.ser594.resumeassistant.infrastructure.converter;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * PDF converter: PDF ↔ MD/TXT (powered by Pandoc and LibreOffice)
 */
@Slf4j
@Component
public class PdfConverter extends AbstractDocumentConverter {

    public PdfConverter() {
        register("pdf", "txt", "html");
        register("txt", "pdf");
        register("md", "pdf");
        register("html", "pdf");
    }

    @Override
    public InputStream convert(InputStream source, String sourceFormat, String targetFormat) throws IOException {
        String sf = sourceFormat.toLowerCase();
        String tf = targetFormat.toLowerCase();

        if (sf.equals(tf)) {
            return new ByteArrayInputStream(source.readAllBytes());
        }

        // PDF to TXT
        if (sf.equals("pdf") && tf.equals("txt")) {
            return pdfToText(source);
        }
        
        // MD to PDF (Pandoc with weasyprint and CJK mainfont)
        if ((sf.equals("md") || sf.equals("markdown")) && tf.equals("pdf")) {
            return ExternalCommandUtils.runPandoc(source, sf, tf, "--pdf-engine=weasyprint -V CJKmainfont=\"Noto Sans SC\"");
        }
        
        // HTML/TXT to PDF (can use LibreOffice or Pandoc depending on simple markup)
        // Since HTML might be styled, weasyprint is ideal.
        if ((sf.equals("txt") || sf.equals("html")) && tf.equals("pdf")) {
            return ExternalCommandUtils.runPandoc(source, sf, tf, "--pdf-engine=weasyprint -V CJKmainfont=\"Noto Sans SC\"");
        }

        throw new IOException("Unsupported conversion in PdfConverter: " + sourceFormat + " -> " + targetFormat);
    }

    /**
     * Extract text from PDF using PDFBox
     */
    private InputStream pdfToText(InputStream pdfStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return toStream(text);
        }
    }
}
