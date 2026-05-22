package io.jobcopilot.resumeassistant.api.common.dto;

import io.jobcopilot.resumeassistant.domain.shared.service.MessageProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Unified API response wrapper enforcing a consistent envelope structure across all endpoints.
 * 统一 API 响应封装，确保所有端点返回一致的信封结构，便于前端错误处理和类型推断
 */
@Getter
@Builder
public class ApiResponse<T> {

    private static final String DEFAULT_SUCCESS_KEY = "success.default";
    /**
     * -- SETTER --
     * Injects the i18n provider at startup; without it success messages fall back to hard-coded English.
     * 在启动时注入国际化提供者；若未注入，成功消息将回退到硬编码英文
     */
    @Setter
    private static MessageProvider messageProvider;

    private final int code;
    private final String message;
    private final T data;

    private static String resolveSuccessMessage() {
        if (messageProvider != null) {
            return messageProvider.getMessage(DEFAULT_SUCCESS_KEY);
        }
        return "Success";
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(resolveSuccessMessage())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
