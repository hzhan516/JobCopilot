package edu.asu.ser594.resumeassistant.domain.shared.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档格式转换器接口
 * Document format converter interface
 */
public interface DocumentFormatConverter {
    /**
     * Check if this converter supports the conversion
     * 检查是否支持该转换
     *
     * @param sourceFormat Source format (pdf, docx, md, txt)
     * @param targetFormat Target format (pdf, docx, md, txt, html)
     */
    boolean supports(String sourceFormat, String targetFormat);

    /**
     * Convert document from source format to target format
     * 执行格式转换
     *
     * @param source       Source input stream
     * @param sourceFormat Source format
     * @param targetFormat Target format
     * @return Converted document as input stream
     * @throws IOException If conversion fails
     */
    InputStream convert(InputStream source, String sourceFormat, String targetFormat) throws IOException;

    /**
     * Get supported target formats for a source format
     * 获取某源格式支持的目标格式
     */
    java.util.List<String> getSupportedTargets(String sourceFormat);
}
