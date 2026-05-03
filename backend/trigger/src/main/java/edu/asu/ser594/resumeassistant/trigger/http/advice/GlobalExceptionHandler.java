package edu.asu.ser594.resumeassistant.trigger.http.advice;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.shared.service.ExceptionMessageResolver;
import edu.asu.ser594.resumeassistant.domain.matching.exception.ResumeVectorNotReadyException;
import edu.asu.ser594.resumeassistant.domain.shared.exception.LocalizedException;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

// 支持国际化的全局异常处理器
// Global exception handler with i18n support
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageProvider messageProvider;
    private final ExceptionMessageResolver exceptionResolver;

    /**
     * 参数校验异常 - 从 messages.properties 读取
     * Parameter validation exception - read from messages.properties
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            // 消息来自 messages.properties，已根据 Accept-Language 本地化
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        // 从 i18n 获取错误摘要
        String summaryMessage = messageProvider.getMessage("validation.failed");

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, summaryMessage, fieldErrors));
    }

    /**
     * 约束校验异常（@Validated）
     * Handle @Validated constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {

        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElseGet(() -> messageProvider.getMessage("error.invalid.request"));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }

    /**
     * 参数类型转换异常
     * Handle method argument type mismatch (e.g., invalid UUID format)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "Unknown";

        log.warn("Parameter type mismatch: {} should be {}", paramName, requiredType);

        String message = String.format("Parameter '%s' must be a valid %s", paramName, requiredType);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }

    /**
     * 认证异常
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
        log.warn("Authentication failed: {}", ex.getErrorType());

        // 通过 Application 层翻译为本地化消息
        String localizedMessage = exceptionResolver.resolve(ex.getErrorType());

        HttpStatus status = switch (ex.getErrorType()) {
            case EMAIL_EXISTS, EMAIL_REGISTERED_WITH_PASSWORD -> HttpStatus.CONFLICT;
            case INVALID_CREDENTIALS, EMAIL_NOT_FOUND, EMAIL_NOT_VERIFIED, TOKEN_EXPIRED, TOKEN_INVALID ->
                    HttpStatus.UNAUTHORIZED;
        };

        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), localizedMessage));
    }

    /**
     * 本地化异常
     * Handle localized exceptions
     */
    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocalizedException(LocalizedException ex) {
        log.warn("Localized exception: {}", ex.getMessageKey());

        String message;
        try {
            message = messageProvider.getMessage(ex.getMessageKey(), ex.getArgs());
        } catch (Exception e) {
            message = ex.getMessageKey();
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }

    /**
     * 简历向量未就绪异常
     * Handle resume vector not ready exception
     */
    @ExceptionHandler(ResumeVectorNotReadyException.class)
    public ResponseEntity<ApiResponse<Void>> handleResumeVectorNotReady(ResumeVectorNotReadyException ex) {
        log.warn("Resume vector not ready: {}", ex.getMessageKey());

        String message;
        try {
            message = messageProvider.getMessage(ex.getMessageKey(), ex.getArgs());
        } catch (Exception e) {
            message = ex.getMessageKey();
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(422, message));
    }

    /**
     * 系统异常
     * Handle system exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("System error", ex);
        String message = messageProvider.getMessage("error.system");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, message));
    }
}
