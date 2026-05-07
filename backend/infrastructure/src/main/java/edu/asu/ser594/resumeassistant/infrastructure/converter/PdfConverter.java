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
 * PDF converter backed by PDFBox for text extraction and Pandoc for format generation.
 * Uses weasyprint with CJK font support when rendering PDFs from markup to handle Chinese characters.
 * PDF 转换器，PDFBox 负责文本提取，Pandoc 负责格式生成；渲染时启用 weasyprint + CJK 字体以支持中文
 */
@Slf4j
@Component
public class PdfConverter extends AbstractDocumentConverter {

    public PdfConverter() {
        register("pdf", "txt", "html", "md", "docx");
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

        if (sf.equals("pdf") && tf.equals("txt")) {
            return pdfToText(source);
        }
        if (sf.equals("pdf") && tf.equals("md")) {
            String text = pdfToTextAsString(source);
            String md = "# Extracted Resume\n\n" + text;
            return toStream(md);
        }
        if (sf.equals("pdf") && tf.equals("docx")) {
            return ExternalCommandUtils.runPandoc(source, "pdf", "docx", null);
        }

        if ((sf.equals("md") || sf.equals("markdown")) && tf.equals("pdf")) {
            return ExternalCommandUtils.runPandoc(source, sf, tf, "--pdf-engine=weasyprint -V CJKmainfont=\"Noto Sans SC\"");
        }

        if ((sf.equals("txt") || sf.equals("html")) && tf.equals("pdf")) {
            return ExternalCommandUtils.runPandoc(source, sf, tf, "--pdf-engine=weasyprint -V CJKmainfont=\"Noto Sans SC\"");
        }

        throw new IOException("Unsupported conversion in PdfConverter: " + sourceFormat + " -> " + targetFormat);
    }

    private InputStream pdfToText(InputStream pdfStream) throws IOException {
        return toStream(pdfToTextAsString(pdfStream));
    }

    private String pdfToTextAsString(InputStream pdfStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
