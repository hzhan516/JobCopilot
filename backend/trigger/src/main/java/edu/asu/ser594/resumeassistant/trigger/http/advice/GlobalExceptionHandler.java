package edu.asu.ser594.resumeassistant.trigger.http.advice;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.domain.shared.exception.DomainException;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import edu.asu.ser594.resumeassistant.api.shared.service.ExceptionMessageResolver;
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

import java.util.HashMap;
import java.util.Map;

// Global exception handler with i18n support
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageProvider messageProvider;
    private final ExceptionMessageResolver exceptionResolver;

    /**
     * 参数校验异常 - 从 messages.properties 读取
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
     * 认证异常
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
        log.warn("Authentication failed: {}", ex.getErrorType());

        // 通过 Application 层翻译为本地化消息
        String localizedMessage = exceptionResolver.resolve(ex.getErrorType());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, localizedMessage));
    }

    /**
     * 通用领域异常
     * Handle generic domain exceptions
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        log.warn("Domain exception: {}", ex.getMessage());

        // 尝试从 i18n 获取消息，如果没有则使用原始消息
        String message;
        try {
            message = messageProvider.getMessage(ex.getMessage());
        } catch (Exception e) {
            message = ex.getMessage();
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
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