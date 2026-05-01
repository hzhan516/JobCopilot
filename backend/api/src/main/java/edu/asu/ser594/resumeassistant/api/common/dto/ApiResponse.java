package edu.asu.ser594.resumeassistant.api.common.dto;

import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

    private static final String DEFAULT_SUCCESS_KEY = "success.default";
    private static MessageProvider messageProvider;

    private final int code;
    private final String message;
    private final T data;

    /**
     * 注入 MessageProvider 以支持国际化。由 ApiResponseConfigurer 在启动时调用。
     * Inject MessageProvider for i18n support. Called by ApiResponseConfigurer on startup.
     */
    public static void setMessageProvider(MessageProvider provider) {
        messageProvider = provider;
    }

    private static String resolveSuccessMessage() {
        if (messageProvider != null) {
            return messageProvider.getMessage(DEFAULT_SUCCESS_KEY);
        }
        return "Success";
    }

    // 带数据的成功响应
    // Success response with data
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(resolveSuccessMessage())
                .data(data)
                .build();
    }

    // 带消息和数据的成功响应
    // Success response with message and data
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    // 仅带消息的错误响应
    // Error response with message only
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    // 带数据的错误响应（例如校验错误）
    // Error response with data (e.g., validation errors)
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
