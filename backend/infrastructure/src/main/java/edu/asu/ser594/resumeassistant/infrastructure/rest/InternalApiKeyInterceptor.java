package edu.asu.ser594.resumeassistant.infrastructure.rest;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Interceptor that injects the internal API key header into all outbound HTTP requests.
 * 拦截器：为所有出站 HTTP 请求注入内部 API Key header。
 * <p>
 * This provides defense-in-depth by ensuring only the backend can call the AI Service
 * REST endpoints, even if the Docker network boundary is compromised.
 * 作为纵深防御手段，确保即使 Docker 网络边界被突破，也只有后端能调用 AI Service REST 端点。
 * <p>
 * The key is read from the {@code INTERNAL_API_KEY} environment variable. When the
 * variable is unset or empty, the interceptor silently does nothing, allowing local
 * development without a shared secret.
 * 密钥从 {@code INTERNAL_API_KEY} 环境变量读取。当变量未设置或为空时，拦截器静默跳过，
 * 允许本地开发无需共享密钥。
 */
@Component
public class InternalApiKeyInterceptor implements ClientHttpRequestInterceptor {

    /**
     * Name of the environment variable that holds the shared internal API key.
     * 保存共享内部 API Key 的环境变量名。
     */
    private static final String ENV_INTERNAL_API_KEY = "INTERNAL_API_KEY";

    /**
     * HTTP header name used to transmit the internal API key.
     * 用于传输内部 API Key 的 HTTP header 名称。
     */
    private static final String HEADER_INTERNAL_API_KEY = "X-Internal-API-Key";

    /**
     * Cached API key read once at instantiation time.
     * 在实例化时一次性读取并缓存的 API Key。
     */
    private final String apiKey;

    /**
     * Constructs the interceptor and reads the API key from the environment.
     * 构造拦截器并从环境变量读取 API Key。
     */
    public InternalApiKeyInterceptor() {
        this.apiKey = System.getenv(ENV_INTERNAL_API_KEY);
    }

    /**
     * Adds the internal API key header to the outgoing request if the key is configured.
     * 若密钥已配置，则为出站请求添加内部 API Key header。
     *
     * @param request   the outgoing HTTP request / 出站 HTTP 请求
     * @param body      the request body / 请求体
     * @param execution the request execution chain / 请求执行链
     * @return the response from the downstream server / 下游服务器的响应
     * @throws IOException in case of I/O errors / 发生 I/O 错误时
     */
    @Override
    public @NotNull ClientHttpResponse intercept(
            @NotNull HttpRequest request,
            byte @NotNull [] body,
            @NotNull ClientHttpRequestExecution execution
    ) throws IOException {
        if (apiKey != null && !apiKey.isBlank()) {
            request.getHeaders().add(HEADER_INTERNAL_API_KEY, apiKey);
        }
        return execution.execute(request, body);
    }
}
