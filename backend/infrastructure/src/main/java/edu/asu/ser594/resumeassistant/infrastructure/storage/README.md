# 文件存储模块 - File Storage Module

本模块提供了统一的文件存储抽象，支持多种存储后端，可通过配置动态切换。

## 支持的存储类型

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| `minio` | MinIO 对象存储 | 生产环境、分布式部署 |
| `local` | 本地文件系统 | 开发环境、单机部署 |
| `s3` | AWS S3 | AWS 云环境 |
| `oss` | 阿里云 OSS | 阿里云环境 |

## 快速切换存储源

### 方式一：修改配置文件

```yaml
storage:
  type: minio  # 切换为: minio | local | s3 | oss
```

### 方式二：环境变量（推荐用于生产环境）

```bash
export STORAGE_TYPE=minio  # 或 local, s3, oss
```

## 配置示例

### 开发环境（MinIO）

```yaml
storage:
  type: minio
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: resumes
```

### 开发环境（本地文件）

```yaml
storage:
  type: local
  local:
    base-path: ./uploads
    resume-path: resumes
    date-subdirectory: true
```

### 生产环境（AWS S3）

```bash
export STORAGE_TYPE=s3
export AWS_S3_REGION=us-west-2
export AWS_S3_ACCESS_KEY=your-access-key
export AWS_S3_SECRET_KEY=your-secret-key
export AWS_S3_BUCKET_NAME=your-bucket-name
```

### 生产环境（阿里云 OSS）

```bash
export STORAGE_TYPE=oss
export ALIYUN_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
export ALIYUN_OSS_ACCESS_KEY_ID=your-access-key-id
export ALIYUN_OSS_ACCESS_KEY_SECRET=your-access-key-secret
export ALIYUN_OSS_BUCKET_NAME=your-bucket-name
export ALIYUN_OSS_CDN_DOMAIN=https://cdn.yourdomain.com
```

## 环境变量清单

### 通用配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `STORAGE_TYPE` | 存储类型 | `minio` |

### MinIO 配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `MINIO_ENDPOINT` | MinIO 服务端点 | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | 访问密钥 | `minioadmin` |
| `MINIO_SECRET_KEY` | 秘密密钥 | `minioadmin` |
| `MINIO_BUCKET_NAME` | 存储桶名称 | `resumes` |
| `MINIO_CONNECT_TIMEOUT` | 连接超时（毫秒） | `5000` |
| `MINIO_WRITE_TIMEOUT` | 写入超时（毫秒） | `60000` |
| `MINIO_READ_TIMEOUT` | 读取超时（毫秒） | `30000` |

### Local 本地存储配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `LOCAL_STORAGE_BASE_PATH` | 存储根目录 | `./uploads` |
| `LOCAL_STORAGE_RESUME_PATH` | 简历子目录 | `resumes` |
| `LOCAL_STORAGE_DATE_SUBDIR` | 是否按日期创建子目录 | `true` |
| `LOCAL_STORAGE_URL_PREFIX` | URL 前缀，用于生成访问链接 | `""` |

### AWS S3 配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `AWS_S3_REGION` | AWS 区域 | `us-east-1` |
| `AWS_S3_ENDPOINT` | 自定义端点（兼容 S3 的第三方存储） | `""` |
| `AWS_S3_ACCESS_KEY` | 访问密钥 | `""` |
| `AWS_S3_SECRET_KEY` | 秘密密钥 | `""` |
| `AWS_S3_BUCKET_NAME` | 存储桶名称 | `""` |
| `AWS_S3_PATH_STYLE` | 路径风格访问 | `false` |
| `AWS_S3_CONNECTION_TIMEOUT` | 连接超时（毫秒） | `5000` |
| `AWS_S3_SOCKET_TIMEOUT` | Socket 超时（毫秒） | `50000` |
| `AWS_S3_MAX_CONNECTIONS` | 最大连接数 | `50` |
| `AWS_S3_ENCRYPTION_ENABLED` | 是否启用加密 | `false` |
| `AWS_S3_KMS_KEY_ID` | KMS 密钥 ID | `""` |

### 阿里云 OSS 配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `ALIYUN_OSS_ENDPOINT` | 访问域名/端点 | `""` |
| `ALIYUN_OSS_ACCESS_KEY_ID` | 访问密钥 ID | `""` |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | 访问密钥密码 | `""` |
| `ALIYUN_OSS_BUCKET_NAME` | 存储桶名称 | `""` |
| `ALIYUN_OSS_CDN_DOMAIN` | 访问域名/CDN 域名 | `""` |
| `ALIYUN_OSS_CONNECTION_TIMEOUT` | 连接超时（毫秒） | `5000` |
| `ALIYUN_OSS_SOCKET_TIMEOUT` | Socket 超时（毫秒） | `50000` |
| `ALIYUN_OSS_MAX_CONNECTIONS` | 最大连接数 | `50` |
| `ALIYUN_OSS_DOWNLOAD_BUFFER` | 下载缓冲区大小（KB） | `64` |
| `ALIYUN_OSS_UPLOAD_PART_SIZE` | 上传分片大小（MB） | `5` |
| `ALIYUN_OSS_SSE` | 服务器端加密方式 | `""` |
| `ALIYUN_OSS_KMS_KEY_ID` | KMS 密钥 ID | `""` |

## 扩展新的存储类型

1. 在 `StorageProperties` 中添加新的配置类
2. 创建新的配置类（如 `S3Config.java`）
3. 创建新的服务实现（如 `S3FileStorageService.java`）
4. 使用 `@ConditionalOnProperty` 注解控制激活条件

```java
@Service
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {
    // 实现...
}
```
