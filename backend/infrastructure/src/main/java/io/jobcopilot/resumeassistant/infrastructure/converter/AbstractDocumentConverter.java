package io.jobcopilot.resumeassistant.infrastructure.converter;

import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Abstract base class for document converters
 * 文档转换器抽象基类
 */
@Slf4j
public abstract class AbstractDocumentConverter implements DocumentFormatConverter {

    protected final Map<String, Set<String>> supportMatrix = new HashMap<>();

    protected void register(String source, String... targets) {
        supportMatrix.computeIfAbsent(source.toLowerCase(), k -> new HashSet<>())
                .addAll(Arrays.stream(targets).map(String::toLowerCase).toList());
    }

    @Override
    public boolean supports(String sourceFormat, String targetFormat) {
        if (sourceFormat == null || targetFormat == null) return false;
        Set<String> targets = supportMatrix.get(sourceFormat.toLowerCase());
        return targets != null && targets.contains(targetFormat.toLowerCase());
    }

    @Override
    public List<String> getSupportedTargets(String sourceFormat) {
        if (sourceFormat == null) return Collections.emptyList();
        Set<String> targets = supportMatrix.get(sourceFormat.toLowerCase());
        return targets != null ? new ArrayList<>(targets) : Collections.emptyList();
    }

    /**
     * 将字符串内容写入 ByteArrayInputStream
     * Write string content to ByteArrayInputStream
     */
    protected InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 将输入流读取为字符串
     * Read input stream to string
     */
    protected String toString(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
