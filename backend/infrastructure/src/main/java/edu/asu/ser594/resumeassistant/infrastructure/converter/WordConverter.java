package edu.asu.ser594.resumeassistant.infrastructure.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Word 转换器：DOCX ↔ MD/PDF，由 Pandoc 和 LibreOffice 提供支持
 * Word converter: DOCX ↔ MD/PDF, powered by Pandoc and LibreOffice
 */
@Slf4j
@Component
public class WordConverter extends AbstractDocumentConverter {

    public WordConverter() {
        register("docx", "md", "txt", "pdf", "html");
        register("md", "docx");
        register("html", "docx");
        register("txt", "docx");
    }

    @Override
    public InputStream convert(InputStream source, String sourceFormat, String targetFormat) throws IOException {
        String sf = sourceFormat.toLowerCase();
        String tf = targetFormat.toLowerCase();

        // DOCX 转 MD / TXT / HTML
        // DOCX to MD / TXT / HTML
        if ((sf.equals("docx") || sf.equals("doc")) && (tf.equals("md") || tf.equals("txt") || tf.equals("html"))) {
            return ExternalCommandUtils.runPandoc(source, sf, tf, null);
        }

        // MD / HTML / TXT 转 DOCX
        // MD / HTML / TXT to DOCX
        if ((sf.equals("md") || sf.equals("markdown") || sf.equals("html") || sf.equals("txt")) && tf.equals("docx")) {
            return ExternalCommandUtils.runPandoc(source, sf, "docx", null);
        }

        // DOCX 转 PDF
        // DOCX to PDF
        if ((sf.equals("docx") || sf.equals("doc")) && tf.equals("pdf")) {
            return ExternalCommandUtils.runLibreOffice(source, sf, "pdf");
        }

        throw new IOException("Unsupported conversion in WordConverter: " + sourceFormat + " -> " + targetFormat);
    }
}
