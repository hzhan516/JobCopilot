package edu.asu.ser594.resumeassistant.trigger.http.advice;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.shared.service.ExceptionMessageResolver;
import edu.asu.ser594.resumeassistant.domain.shared.exception.BusinessException;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler Unit Tests
 * 
 * Tests the global exception handler:
 * - Business exception handling
 * - Validation exception handling
 * - Generic exception handling
 * - Response structure
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private AuthException authException;

    @Mock
    private MessageProvider messageProvider;

    @Mock
    private ExceptionMessageResolver exceptionResolver;

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    @DisplayName("Should handle auth exception with EMAIL_EXISTS type")
    void shouldHandleAuthExceptionWithEmailExistsType() {
        // Given
        when(authException.getErrorType()).thenReturn(AuthException.ErrorType.EMAIL_EXISTS);
        when(authException.getMessage()).thenReturn("Email already exists");
        when(exceptionResolver.resolve(AuthException.ErrorType.EMAIL_EXISTS)).thenReturn("Email already exists");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(authException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should handle auth exception with INVALID_CREDENTIALS type")
    void shouldHandleAuthExceptionWithInvalidCredentialsType() {
        // Given
        when(authException.getErrorType()).thenReturn(AuthException.ErrorType.INVALID_CREDENTIALS);
        when(authException.getMessage()).thenReturn("Invalid credentials");
        when(exceptionResolver.resolve(AuthException.ErrorType.INVALID_CREDENTIALS)).thenReturn("Invalid credentials");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(authException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should handle auth exception with TOKEN_EXPIRED type")
    void shouldHandleAuthExceptionWithTokenExpiredType() {
        // Given
        when(authException.getErrorType()).thenReturn(AuthException.ErrorType.TOKEN_EXPIRED);
        when(authException.getMessage()).thenReturn("Token expired");
        when(exceptionResolver.resolve(AuthException.ErrorType.TOKEN_EXPIRED)).thenReturn("Token expired");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(authException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should handle auth exception with TOKEN_INVALID type")
    void shouldHandleAuthExceptionWithTokenInvalidType() {
        // Given
        when(authException.getErrorType()).thenReturn(AuthException.ErrorType.TOKEN_INVALID);
        when(authException.getMessage()).thenReturn("Token invalid");
        when(exceptionResolver.resolve(AuthException.ErrorType.TOKEN_INVALID)).thenReturn("Token invalid");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(authException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should handle generic business exception")
    void shouldHandleGenericBusinessException() {
        // Given
        BusinessException businessException = new BusinessException("BUSINESS_ERROR", "Business rule violated") {};
        when(messageProvider.getMessage("BUSINESS_ERROR")).thenReturn("Business rule violated");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDomainException(businessException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("Business rule violated");
    }

    @Test
    @DisplayName("Should handle validation exception")
    void shouldHandleValidationException() {
        // Given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "email", "Email is required"));
        bindingResult.addError(new FieldError("object", "password", "Password must be at least 8 characters"));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        when(messageProvider.getMessage("validation.failed")).thenReturn("Validation failed");

        // When
        ResponseEntity<ApiResponse<java.util.Map<String, String>>> response = exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("Validation failed");
    }

    @Test
    @DisplayName("Should handle generic exception")
    void shouldHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");
        when(messageProvider.getMessage("error.system")).thenReturn("System error occurred");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should return structured error response")
    void shouldReturnStructuredErrorResponse() {
        // Given
        when(authException.getErrorType()).thenReturn(AuthException.ErrorType.EMAIL_EXISTS);
        when(authException.getMessage()).thenReturn("Email already exists");
        when(exceptionResolver.resolve(AuthException.ErrorType.EMAIL_EXISTS)).thenReturn("Email already exists");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(authException);

        // Then
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode() == 200).isFalse();
        assertThat(body.getCode()).isNotEqualTo(200);
        assertThat(body.getMessage()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle null exception message")
    void shouldHandleNullExceptionMessage() {
        // Given
        Exception exception = new RuntimeException();
        when(messageProvider.getMessage("error.system")).thenReturn("System error occurred");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isNotEmpty();
    }
}
