package edu.asu.ser594.resumeassistant.infrastructure.converter;

import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Composite document converter that routes to specific converters
 * 组合文档转换器 - 路由到具体实现
 * <p>
 * 支持直接转换、链式转换（通过中间格式）以及纯文本降级回退。
 * Supports direct conversion, chain conversion (via intermediate format),
 * and fallback to built-in text converters when external commands fail.
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class CompositeDocumentConverter implements DocumentFormatConverter {

    private final List<DocumentFormatConverter> converters;

    private static final List<String> CHAIN_INTERMEDIATES = List.of("md", "txt");

    @Override
    public InputStream convert(InputStream source, String sourceFormat, String targetFormat) throws IOException {
        String sf = normalize(sourceFormat);
        String tf = normalize(targetFormat);

        if (sf.equals(tf)) {
            return source;
        }

        // 1. 尝试直接转换
        // 1. Try direct conversion
        for (DocumentFormatConverter converter : converters) {
            if (converter instanceof CompositeDocumentConverter) {
                continue;
            }
            if (converter.supports(sf, tf)) {
                try {
                    return converter.convert(source, sf, tf);
                } catch (IOException e) {
                    log.warn("Direct conversion failed: {} -> {}, trying fallback. Error: {}",
                            sf, tf, e.getMessage());
                    // 继续尝试降级/链式转换
                    // Continue to fallback/chain conversion
                }
            }
        }

        // 2. 尝试链式转换（source -> intermediate -> target）
        // 2. Try chain conversion
        byte[] sourceBytes = readAllBytes(source);
        for (String intermediate : CHAIN_INTERMEDIATES) {
            if (intermediate.equals(sf) || intermediate.equals(tf)) {
                continue;
            }
            try {
                InputStream result = tryChainConvert(sourceBytes, sf, intermediate, tf);
                if (result != null) {
                    log.info("Chain conversion succeeded: {} -> {} -> {}", sf, intermediate, tf);
                    return result;
                }
            } catch (IOException e) {
                log.debug("Chain conversion failed: {} -> {} -> {}: {}", sf, intermediate, tf, e.getMessage());
            }
        }

        // 3. 纯文本降级：如果涉及 md/html/txt，尝试使用内置的 MarkdownConverter
        // 3. Pure-text fallback: try built-in MarkdownConverter for md/html/txt
        if (isPureTextFormat(sf) && isPureTextFormat(tf)) {
            InputStream fallback = tryPureTextFallback(sourceBytes, sf, tf);
            if (fallback != null) {
                return fallback;
            }
        }

        throw new IOException("No converter found for: " + sourceFormat + " -> " + targetFormat);
    }

    @Override
    public boolean supports(String sourceFormat, String targetFormat) {
        String sf = normalize(sourceFormat);
        String tf = normalize(targetFormat);
        if (sf.equals(tf)) {
            return true;
        }

        // 直接支持
        boolean direct = converters.stream()
                .filter(c -> !(c instanceof CompositeDocumentConverter))
                .anyMatch(c -> c.supports(sf, tf));
        if (direct) {
            return true;
        }

        // 链式支持
        for (String intermediate : CHAIN_INTERMEDIATES) {
            if (intermediate.equals(sf) || intermediate.equals(tf)) {
                continue;
            }
            boolean step1 = converters.stream()
                    .filter(c -> !(c instanceof CompositeDocumentConverter))
                    .anyMatch(c -> c.supports(sf, intermediate));
            boolean step2 = converters.stream()
                    .filter(c -> !(c instanceof CompositeDocumentConverter))
                    .anyMatch(c -> c.supports(intermediate, tf));
            if (step1 && step2) {
                return true;
            }
        }

        // 纯文本降级
        return isPureTextFormat(sf) && isPureTextFormat(tf);
    }

    @Override
    public List<String> getSupportedTargets(String sourceFormat) {
        String sf = normalize(sourceFormat);
        return converters.stream()
                .filter(c -> !(c instanceof CompositeDocumentConverter))
                .flatMap(c -> c.getSupportedTargets(sf).stream())
                .distinct()
                .toList();
    }

    /**
     * 尝试链式转换
     * Try chain conversion
     */
    private InputStream tryChainConvert(byte[] sourceBytes, String sf, String intermediate, String tf)
            throws IOException {
        DocumentFormatConverter step1Converter = null;
        DocumentFormatConverter step2Converter = null;

        for (DocumentFormatConverter converter : converters) {
            if (converter instanceof CompositeDocumentConverter) {
                continue;
            }
            if (step1Converter == null && converter.supports(sf, intermediate)) {
                step1Converter = converter;
            }
            if (step2Converter == null && converter.supports(intermediate, tf)) {
                step2Converter = converter;
            }
        }

        if (step1Converter == null || step2Converter == null) {
            return null;
        }

        try (InputStream intermediateStream = step1Converter.convert(new ByteArrayInputStream(sourceBytes), sf, intermediate)) {
            return step2Converter.convert(intermediateStream, intermediate, tf);
        }
    }

    /**
     * 纯文本降级回退
     * Pure-text fallback using MarkdownConverter logic
     */
    private InputStream tryPureTextFallback(byte[] sourceBytes, String sf, String tf) {
        for (DocumentFormatConverter converter : converters) {
            if (converter instanceof CompositeDocumentConverter) {
                continue;
            }
            if (converter instanceof MarkdownConverter && converter.supports(sf, tf)) {
                try {
                    return converter.convert(new ByteArrayInputStream(sourceBytes), sf, tf);
                } catch (IOException e) {
                    log.warn("MarkdownConverter fallback failed: {} -> {}", sf, tf, e);
                }
            }
        }
        return null;
    }

    private boolean isPureTextFormat(String format) {
        return "md".equals(format) || "markdown".equals(format)
                || "html".equals(format) || "txt".equals(format) || "text".equals(format);
    }

    private byte[] readAllBytes(InputStream source) throws IOException {
        return source.readAllBytes();
    }

    /**
     * 规范化格式名称
     * Normalize format name
     */
    private String normalize(String format) {
        if (format == null) return "";
        String f = format.toLowerCase();
        return switch (f) {
            case "markdown" -> "md";
            case "word" -> "docx";
            case "doc" -> "docx"; // Treat DOC as DOCX for simplicity
            // 为简单起见，将 DOC 视为 DOCX
            case "text" -> "txt";
            default -> f;
        };
    }
}
