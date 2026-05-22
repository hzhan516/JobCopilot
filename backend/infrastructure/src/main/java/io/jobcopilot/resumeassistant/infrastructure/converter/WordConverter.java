package io.jobcopilot.resumeassistant.infrastructure.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * DOCX converter delegating to Pandoc for markup formats and LibreOffice for PDF output.
 * Pandoc handles structural fidelity better for MD/HTML, while LibreOffice preserves
 * visual layout when producing PDFs from Word documents.
 * DOCX 转换器，Pandoc 处理 MD/HTML 以保持结构保真，LibreOffice 处理 PDF 以保留视觉布局
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

        if ((sf.equals("docx") || sf.equals("doc")) && (tf.equals("md") || tf.equals("txt") || tf.equals("html"))) {
            return ExternalCommandUtils.runPandoc(source, sf, tf, null);
        }

        if ((sf.equals("md") || sf.equals("markdown") || sf.equals("html") || sf.equals("txt")) && tf.equals("docx")) {
            return ExternalCommandUtils.runPandoc(source, sf, "docx", null);
        }

        if ((sf.equals("docx") || sf.equals("doc")) && tf.equals("pdf")) {
            return ExternalCommandUtils.runLibreOffice(source, sf, "pdf");
        }

        throw new IOException("Unsupported conversion in WordConverter: " + sourceFormat + " -> " + targetFormat);
    }
}
