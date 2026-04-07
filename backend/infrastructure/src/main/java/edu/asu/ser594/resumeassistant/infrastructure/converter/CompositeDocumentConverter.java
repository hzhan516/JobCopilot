package edu.asu.ser594.resumeassistant.infrastructure.converter;

import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Composite document converter that routes to specific converters
 * 组合文档转换器 - 路由到具体实现
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class CompositeDocumentConverter implements DocumentFormatConverter {

    private final List<DocumentFormatConverter> converters;

    @Override
    public InputStream convert(InputStream source, String sourceFormat, String targetFormat) throws IOException {
        String sf = normalize(sourceFormat);
        String tf = normalize(targetFormat);

        if (sf.equals(tf)) {
            return source;
        }

        // 查找具体实现（排除自己）
        for (DocumentFormatConverter converter : converters) {
            if (converter instanceof CompositeDocumentConverter) {
                continue; // 跳过自己
            }
            if (converter.supports(sf, tf)) {
                return converter.convert(source, sf, tf);
            }
        }

        throw new IOException("No converter found for: " + sourceFormat + " -> " + targetFormat);
    }

    @Override
    public boolean supports(String sourceFormat, String targetFormat) {
        return converters.stream()
                .filter(c -> !(c instanceof CompositeDocumentConverter)) // 排除自己
                .anyMatch(c -> c.supports(sourceFormat, targetFormat));
    }

    @Override
    public List<String> getSupportedTargets(String sourceFormat) {
        return converters.stream()
                .flatMap(c -> c.getSupportedTargets(sourceFormat).stream())
                .distinct()
                .toList();
    }

    /**
     * Normalize format name
     */
    private String normalize(String format) {
        if (format == null) return "";
        String f = format.toLowerCase();
        return switch (f) {
            case "markdown" -> "md";
            case "word" -> "docx";
            case "doc" -> "docx"; // Treat DOC as DOCX for simplicity
            case "text" -> "txt";
            default -> f;
        };
    }
}
