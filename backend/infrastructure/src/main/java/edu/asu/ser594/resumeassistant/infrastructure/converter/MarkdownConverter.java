package edu.asu.ser594.resumeassistant.infrastructure.converter;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Built-in converter for lightweight markup formats (MD, HTML, TXT).
 * Provides a fallback path when external tools like Pandoc are unavailable.
 * 内置轻量标记格式转换器（MD/HTML/TXT），作为 Pandoc 等外部工具不可用时的降级方案
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

        // PDF conversion from MD is handled via chain conversion (MD -> TXT -> PDF) in ResumeApplicationService
        // MD 转 PDF 通过链式转换在 ResumeApplicationService 中处理
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

        if ((sf.equals("md") || sf.equals("markdown")) && tf.equals("html")) {
            result = markdownToHtml(content);
        } else if ((sf.equals("md") || sf.equals("markdown")) && tf.equals("txt")) {
            result = markdownToText(content);
        } else if (sf.equals("html") && (tf.equals("md") || tf.equals("markdown"))) {
            result = htmlToMarkdown(content);
        } else if (sf.equals("txt") && (tf.equals("md") || tf.equals("markdown"))) {
            result = "```\n" + content + "\n```";
        } else if (sf.equals("html") && tf.equals("txt")) {
            result = htmlToText(content);
        } else if (sf.equals("txt") && tf.equals("html")) {
            result = "<pre>" + escapeHtml(content) + "</pre>";
        } else {
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
        // Best-effort regex-based conversion; sufficient for simple resume HTML fragments
        // 基于正则的简化转换，适用于简历片段等简单 HTML
        return html.replaceAll("<br\\s*/?>", "\n")
                .replace("<p>", "\n\n")
                .replace("</p>", "")
                .replaceAll("<strong>|</strong>", "**")
                .replaceAll("<em>|</em>", "*")
                .replaceAll("<[^>]+>", "") // Strip remaining tags | 移除剩余标签
                .trim();
    }

    private String htmlToText(String html) {
        return html.replaceAll("<br\\s*/?>", "\n")
                .replace("</p>", "\n\n")
                .replaceAll("<[^>]+>", "") // Strip all tags | 移除所有标签
                .trim();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
