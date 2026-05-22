package io.jobcopilot.resumeassistant.infrastructure.converter;

import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Primary document converter that orchestrates multiple conversion strategies.
 * Attempts direct conversion first, then chain conversion through intermediate formats,
 * and finally falls back to built-in text converters to maximize format coverage.
 * 主文档转换器，协调多种转换策略：优先直接转换，其次通过中间格式链式转换，
 * 最后降级到内置文本转换器，以最大化支持的格式覆盖范围
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
                }
            }
        }

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

        boolean direct = converters.stream()
                .filter(c -> !(c instanceof CompositeDocumentConverter))
                .anyMatch(c -> c.supports(sf, tf));
        if (direct) {
            return true;
        }

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

    private String normalize(String format) {
        if (format == null) return "";
        String f = format.toLowerCase();
        return switch (f) {
            case "markdown" -> "md";
            case "word" -> "docx";
            case "doc" -> "docx"; // Treat DOC as DOCX for simplicity | 将 DOC 视为 DOCX 以简化处理
            case "text" -> "txt";
            default -> f;
        };
    }
}
