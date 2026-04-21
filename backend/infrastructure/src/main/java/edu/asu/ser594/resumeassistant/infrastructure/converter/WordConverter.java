package edu.asu.ser594.resumeassistant.infrastructure.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Word converter: DOCX ↔ MD/PDF, powered by Pandoc and LibreOffice
 */
@Slf4j
@Component
public class WordConverter extends AbstractDocumentConverter {

    public WordConverter() {
        register("docx", "md", "txt", "pdf");
        register("md", "docx");
    }

    @Override
    public InputStream convert(InputStream source, String sourceFormat, String targetFormat) throws IOException {
        String sf = sourceFormat.toLowerCase();
        String tf = targetFormat.toLowerCase();

        // DOCX to MD or TXT
        if ((sf.equals("docx") || sf.equals("doc")) && (tf.equals("md") || tf.equals("txt"))) {
            return ExternalCommandUtils.runPandoc(source, sf, tf, null);
        }
        
        // MD to DOCX
        if ((sf.equals("md") || sf.equals("markdown")) && tf.equals("docx")) {
            return ExternalCommandUtils.runPandoc(source, "md", "docx", null);
        }
        
        // DOCX to PDF
        if ((sf.equals("docx") || sf.equals("doc")) && tf.equals("pdf")) {
            return ExternalCommandUtils.runLibreOffice(source, sf, "pdf");
        }

        throw new IOException("Unsupported conversion in WordConverter: " + sourceFormat + " -> " + targetFormat);
    }
}
