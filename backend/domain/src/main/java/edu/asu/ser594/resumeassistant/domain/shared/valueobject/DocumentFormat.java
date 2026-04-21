package edu.asu.ser594.resumeassistant.domain.shared.valueobject;

import java.util.Optional;

/**
 * 文档格式值对象
 * Document Format Value Object
 * 
 * 封装格式解析、MIME类型映射以及导出文件名生成的领域规则
 */
public class DocumentFormat {

    private final String format;
    private final String mimeType;

    private DocumentFormat(String format, String mimeType) {
        this.format = format.toLowerCase();
        this.mimeType = mimeType;
    }

    public static DocumentFormat fromMimeType(String mimeType) {
        if (mimeType == null) {
            return new DocumentFormat("bin", "application/octet-stream");
        }
        return switch (mimeType.toLowerCase()) {
            case "application/pdf" -> new DocumentFormat("pdf", mimeType);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword" -> new DocumentFormat("docx", mimeType);
            case "text/markdown", "text/x-markdown" -> new DocumentFormat("md", mimeType);
            case "text/plain" -> new DocumentFormat("txt", mimeType);
            case "text/html" -> new DocumentFormat("html", mimeType);
            default -> new DocumentFormat("bin", mimeType);
        };
    }

    public static DocumentFormat fromFormatString(String format) {
        if (format == null) {
            return new DocumentFormat("bin", "application/octet-stream");
        }
        String f = format.toLowerCase();
        if (f.startsWith(".")) {
            f = f.substring(1);
        }
        
        return switch (f) {
            case "pdf" -> new DocumentFormat("pdf", "application/pdf");
            case "docx", "doc", "word" -> new DocumentFormat("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "md", "markdown" -> new DocumentFormat("md", "text/markdown");
            case "txt", "text" -> new DocumentFormat("txt", "text/plain");
            case "html" -> new DocumentFormat("html", "text/html");
            default -> new DocumentFormat(f, "application/octet-stream");
        };
    }

    public String getFormat() {
        return format;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String generateOutputFileName(String originalFileName) {
        String baseName = Optional.ofNullable(originalFileName).orElse("resume");
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        return baseName + "." + format;
    }

    public boolean isSameFormat(String targetFormat) {
        if (targetFormat == null) return false;
        return this.format.equals(fromFormatString(targetFormat).getFormat());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DocumentFormat)) return false;
        DocumentFormat that = (DocumentFormat) obj;
        return format.equals(that.format);
    }

    @Override
    public int hashCode() {
        return format.hashCode();
    }
}
