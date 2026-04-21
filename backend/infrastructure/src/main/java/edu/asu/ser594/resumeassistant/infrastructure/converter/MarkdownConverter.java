package edu.asu.ser594.resumeassistant.infrastructure.converter;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Markdown converter: MD ↔ HTML ↔ TXT
 * Markdown转换器
 */
@Slf4j
@Component
public class MarkdownConverter extends AbstractDocumentConverter {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownConverter() {
        MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        // Register supported conversions
        // Note: MD to PDF is handled via chain conversion (MD -> TXT -> PDF) in ResumeApplicationService
        register("md", "html", "txt");
        register("markdown", "html", "txt");
        register("html", "md", "txt");
        register("txt", "md", "html");
    }

    @Override
    public InputStream convert(InputStream source, String sourceFormat, String targetFormat) throws IOException {
        String content = toString(source);
        String result;

        String sf = sourceFormat.toLowerCase();
        String tf = targetFormat.toLowerCase();

        if (sf.equals(tf)) {
            return toStream(content);
        }

        // MD to HTML
        if ((sf.equals("md") || sf.equals("markdown")) && tf.equals("html")) {
            result = markdownToHtml(content);
        }
        // MD to TXT
        else if ((sf.equals("md") || sf.equals("markdown")) && tf.equals("txt")) {
            result = markdownToText(content);
        }
        // HTML to MD
        else if (sf.equals("html") && (tf.equals("md") || tf.equals("markdown"))) {
            result = htmlToMarkdown(content);
        }
        // TXT to MD (wrap as code block or plain)
        else if (sf.equals("txt") && (tf.equals("md") || tf.equals("markdown"))) {
            result = "```\n" + content + "\n```";
        }
        // HTML to TXT
        else if (sf.equals("html") && tf.equals("txt")) {
            result = htmlToText(content);
        }
        // TXT to HTML
        else if (sf.equals("txt") && tf.equals("html")) {
            result = "<pre>" + escapeHtml(content) + "</pre>";
        }
        else {
            throw new IOException("Unsupported conversion: " + sourceFormat + " -> " + targetFormat);
        }

        return toStream(result);
    }

    private String markdownToHtml(String markdown) {
        return renderer.render(parser.parse(markdown));
    }

    private String markdownToText(String markdown) {
        String html = markdownToHtml(markdown);
        return htmlToText(html);
    }

    private String htmlToMarkdown(String html) {
        // Simple HTML to MD conversion (can be enhanced)
        return html.replaceAll("<br\\s*/?>", "\n")
                .replaceAll("<p>", "\n\n")
                .replaceAll("</p>", "")
                .replaceAll("<strong>|</strong>", "**")
                .replaceAll("<em>|</em>", "*")
                .replaceAll("<[^>]+>", "") // Remove remaining tags
                .trim();
    }

    private String htmlToText(String html) {
        return html.replaceAll("<br\\s*/?>", "\n")
                .replaceAll("</p>", "\n\n")
                .replaceAll("<[^>]+>", "") // Remove all tags
                .trim();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
