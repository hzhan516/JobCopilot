package edu.asu.ser594.resumeassistant.trigger.http.advice;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.shared.service.ExceptionMessageResolver;
import edu.asu.ser594.resumeassistant.domain.job.exception.JobContentNotReadyException;
import edu.asu.ser594.resumeassistant.domain.matching.exception.ResumeVectorNotReadyException;
import edu.asu.ser594.resumeassistant.domain.shared.exception.AiServiceUnavailableException;
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

/**
 * Centralized exception translation layer converting domain and framework errors into standardized,
 * localized API responses while preserving appropriate HTTP semantics.
 * 集中式异常转换层，将领域与框架错误映射为标准化的本地化 API 响应，同时保持恰当的 HTTP 语义
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageProvider messageProvider;
    private final ExceptionMessageResolver exceptionResolver;

    /**
     * Aggregates field-level validation failures into a single structured payload so the frontend
     * can highlight invalid inputs without making multiple round-trips.
     * 聚合字段级校验失败为单一结构化负载，使前端能够一次性定位所有无效输入
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            // already localized by Spring based on Accept-Language | 已由 Spring 根据 Accept-Language 完成本地化
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        String summaryMessage = messageProvider.getMessage("validation.failed");

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, summaryMessage, fieldErrors));
    }

    /**
     * Handles constraint violations that surface outside form objects, such as path or query parameter checks.
     * 处理表单对象之外的约束违规，例如路径参数或查询参数的校验失败
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
     * Produces a clear, actionable message when type conversion fails so API consumers know the expected format.
     * 在类型转换失败时返回明确且可操作的消息，使 API 调用方了解期望的数据格式
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
     * Maps each authentication error subtype to the correct HTTP status and resolves a localized message
     * via the application-layer resolver rather than exposing raw enum names.
     * 将每种认证错误子类型映射到正确的 HTTP 状态码，并通过应用层解析器获取本地化消息，避免暴露原始枚举名
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
        log.warn("Authentication failed: {}", ex.getErrorType());

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
     * Resolves message keys dynamically so business exceptions can carry i18n identifiers instead of hard-coded text.
     * 动态解析消息键，使业务异常能够携带国际化标识而非硬编码文本，支持多语言扩展
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
     * Translates transient AI service failures into 503 so clients can implement retry or graceful degradation.
     * 将 AI 服务的瞬时故障转换为 503，使客户端能够实现重试或优雅降级策略
     */
    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiServiceUnavailable(AiServiceUnavailableException ex) {
        log.warn("AI service unavailable: {}", ex.getMessageKey());

        String message;
        try {
            message = messageProvider.getMessage(ex.getMessageKey(), ex.getArgs());
        } catch (Exception e) {
            message = ex.getMessageKey();
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, message));
    }

    /**
     * Returns 422 when asynchronous job parsing has not yet finished, signaling the client to poll later.
     * 当异步职位解析尚未完成时返回 422，提示客户端稍后重试轮询
     */
    @ExceptionHandler(JobContentNotReadyException.class)
    public ResponseEntity<ApiResponse<Void>> handleJobContentNotReady(JobContentNotReadyException ex) {
        log.warn("Job content not ready: {}", ex.getMessageKey());

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
     * Returns 422 when the resume vector generation pipeline is still running, preventing premature matching.
     * 当简历向量生成流水线仍在运行时返回 422，阻止过早的匹配计算
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
     * Fallback handler ensuring unanticipated errors never propagate raw stack traces to API consumers.
     * 兜底处理器，确保未预期的错误不会将原始堆栈跟踪暴露给 API 调用方
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("System error", ex);
        String message = messageProvider.getMessage("error.system");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, message));
    }
}
