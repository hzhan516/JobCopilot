package edu.asu.ser594.resumeassistant.infrastructure.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统一文件存储配置属性
 * Unified file storage configuration properties
 * 支持: minio, local, s3, oss
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * 存储类型: minio, local, s3, oss
     * Storage type: minio, local, s3, oss
     */
    private String type = "minio";

    /**
     * MinIO 配置
     */
    private Minio minio = new Minio();

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * AWS S3 配置
     */
    private S3 s3 = new S3();

    /**
     * 阿里云 OSS 配置
     */
    private Oss oss = new Oss();

    @Data
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucketName = "resumes";
        private long connectTimeout = 5000;
        private long writeTimeout = 60000;
        private long readTimeout = 30000;
    }

    @Data
    public static class Local {
        /**
         * 文件存储根目录
         */
        private String basePath = "./uploads";

        /**
         * 简历文件子目录
         */
        private String resumePath = "resumes";

        /**
         * 是否按日期创建子目录
         */
        private boolean dateSubdirectory = true;

        /**
         * 访问URL前缀（用于生成可访问链接）
         */
        private String urlPrefix = "";
    }

    @Data
    public static class S3 {
        /**
         * AWS 区域
         */
        private String region = "us-east-1";

        /**
         * 自定义端点（用于兼容 S3 的第三方存储）
         */
        private String endpoint = "";

        /**
         * 访问密钥
         */
        private String accessKey = "";

        /**
         * 秘密密钥
         */
        private String secretKey = "";

        /**
         * 存储桶名称
         */
        private String bucketName = "";

        /**
         * 是否使用 Path-Style 访问
         */
        private boolean pathStyleAccess = false;

        /**
         * 连接超时（毫秒）
         */
        private int connectionTimeout = 5000;

        /**
         * Socket 超时（毫秒）
         */
        private int socketTimeout = 50000;

        /**
         * 最大连接数
         */
        private int maxConnections = 50;

        /**
         * 是否启用服务器端加密
         */
        private boolean encryptionEnabled = false;

        /**
         * KMS 密钥 ID
         */
        private String kmsKeyId = "";
    }

    @Data
    public static class Oss {
        /**
         * 访问域名/端点
         */
        private String endpoint = "";

        /**
         * 访问密钥 ID
         */
        private String accessKeyId = "";

        /**
         * 访问密钥密码
         */
        private String accessKeySecret = "";

        /**
         * 存储桶名称
         */
        private String bucketName = "";

        /**
         * 访问域名/CDN域名（用于生成访问 URL）
         */
        private String cdnDomain = "";

        /**
         * 连接超时（毫秒）
         */
        private int connectionTimeout = 5000;

        /**
         * Socket 超时（毫秒）
         */
        private int socketTimeout = 50000;

        /**
         * 最大连接数
         */
        private int maxConnections = 50;

        /**
         * 下载缓冲区大小（KB）
         */
        private int downloadBufferSize = 64;

        /**
         * 上传分片大小（MB）
         */
        private int uploadPartSize = 5;

        /**
         * 服务器端加密方式: AES256, KMS, SM4
         */
        private String serverSideEncryption = "";

        /**
         * KMS 密钥 ID
         */
        private String kmsKeyId = "";
    }
}
