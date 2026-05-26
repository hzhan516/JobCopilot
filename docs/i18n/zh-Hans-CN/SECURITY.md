# 安全策略

> English version: [SECURITY.md](../../../SECURITY.md)

## 支持的版本

| 版本    | 是否支持 |
|--------:|:--------:|
| 1.0.x   | ✅       |

## 报告安全漏洞

如果您在 ResumeAssistant 中发现了安全漏洞，请通过电子邮件私下向项目维护者报告。**请勿**公开提交 Issue。

**联系方式：** 请通过项目指定的安全联系人（维护者邮箱或私密消息渠道）取得联系。

我们承诺在 **48 小时内** 回复，并与您一起验证、评估优先级并修复问题。

## 安全实践

### 认证与授权
- 所有服务间通信在生产环境中都需要 `X-Internal-API-Key` 请求头验证。
- 后端 JWT 令牌使用可配置的密钥；生产构建中不存在硬编码的默认密钥。
- 刷新令牌通过 `HttpOnly`、`SameSite=Strict`、`Secure` Cookie 传输。

### 数据保护
- 文件上传经过 MIME 类型验证和文件名清理（防止路径遍历攻击）。
- S3/MinIO 预签名 URL 的可配置过期时间（默认 7 天，可通过 `storage.presigned-url-expiration-hours` 覆盖）。
- 敏感日志（令牌、凭据）会被脱敏或排除在应用日志之外。

### 限流与资源保护
- Embedding 接口强制执行批量大小限制（`EMBEDDING_MAX_BATCH_SIZE`）和单条文本长度上限（`EMBEDDING_MAX_TEXT_LENGTH`）。
- 验证码验证使用加固的客户端 IP 检测机制（忽略来自不可信来源的 `X-Forwarded-For`）。

### 基础设施
- Actuator 端点仅公开暴露 `/health` 和 `/info`；其余所有 actuator 路径均被拒绝访问。
- MQ 消费者实现幂等性检查，防止重复处理。
- 非开发环境在启动时强制要求配置 `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY` 和 `INTERNAL_API_KEY`。

## 披露政策

- 报告者在 48 小时内收到确认。
- 补丁在内部准备并验证。
- 修复在下一个补丁版本中发布。
- 公开披露（CVE/安全公告）在 30 天后与报告者协调进行，或经双方同意提前披露。

## 已知限制

- 开发环境配置（`ENV=dev`）出于本地调试目的有意放宽安全检查。**切勿将 `dev` 部署到生产环境。**
- Vertex AI 的 Google Cloud 凭据应通过密钥管理系统（Kubernetes secrets、Vault 等）挂载，而不是提交到代码仓库中。

---

*最后更新：2026-05-26*
