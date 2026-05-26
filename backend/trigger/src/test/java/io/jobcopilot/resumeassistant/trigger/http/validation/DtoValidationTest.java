package io.jobcopilot.resumeassistant.trigger.http.validation;

import io.jobcopilot.resumeassistant.api.user.dto.request.CaptchaVerifyRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.SendVerificationCodeRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Controller DTO 校验边界测试
 * Controller DTO validation boundary tests
 * <p>
 * 覆盖 @NotBlank / @Email / @Size / @NotNull 的边界场景
 * Covers boundary scenarios for @NotBlank / @Email / @Size / @NotNull
 */
@DisplayName("Controller DTO Validation Boundary Tests / Controller DTO 校验边界测试")
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ==================== RegisterByEmailRequest ====================

    @Test
    @DisplayName("Should reject blank email in register request / 注册请求中空邮箱应被拒绝")
    void shouldRejectBlankEmailInRegisterRequest() {
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email("")
                .password("password123")
                .build();

        Set<ConstraintViolation<RegisterByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("email");
    }

    @Test
    @DisplayName("Should reject invalid email format in register request / 注册请求中无效邮箱格式应被拒绝")
    void shouldRejectInvalidEmailFormatInRegisterRequest() {
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email("not-an-email")
                .password("password123")
                .build();

        Set<ConstraintViolation<RegisterByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("email");
    }

    @Test
    @DisplayName("Should reject short password in register request / 注册请求中过短密码应被拒绝")
    void shouldRejectShortPasswordInRegisterRequest() {
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email("test@example.com")
                .password("12345")
                .build();

        Set<ConstraintViolation<RegisterByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("password");
    }

    @Test
    @DisplayName("Should reject long password in register request / 注册请求中过长密码应被拒绝")
    void shouldRejectLongPasswordInRegisterRequest() {
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email("test@example.com")
                .password("a".repeat(33))
                .build();

        Set<ConstraintViolation<RegisterByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("password");
    }

    @Test
    @DisplayName("Should reject blank password in register request / 注册请求中空密码应被拒绝")
    void shouldRejectBlankPasswordInRegisterRequest() {
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email("test@example.com")
                .password("")
                .build();

        Set<ConstraintViolation<RegisterByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("password");
    }

    @Test
    @DisplayName("Should accept valid register request / 合法注册请求应被接受")
    void shouldAcceptValidRegisterRequest() {
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        Set<ConstraintViolation<RegisterByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // ==================== LoginByEmailRequest ====================

    @Test
    @DisplayName("Should reject blank email in login request / 登录请求中空邮箱应被拒绝")
    void shouldRejectBlankEmailInLoginRequest() {
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email("")
                .password("password123")
                .build();

        Set<ConstraintViolation<LoginByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("email");
    }

    @Test
    @DisplayName("Should reject invalid email format in login request / 登录请求中无效邮箱格式应被拒绝")
    void shouldRejectInvalidEmailFormatInLoginRequest() {
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email("invalid-email")
                .password("password123")
                .build();

        Set<ConstraintViolation<LoginByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("email");
    }

    @Test
    @DisplayName("Should reject blank password in login request / 登录请求中空密码应被拒绝")
    void shouldRejectBlankPasswordInLoginRequest() {
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email("test@example.com")
                .password("")
                .build();

        Set<ConstraintViolation<LoginByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("password");
    }

    @Test
    @DisplayName("Should accept valid login request / 合法登录请求应被接受")
    void shouldAcceptValidLoginRequest() {
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        Set<ConstraintViolation<LoginByEmailRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // ==================== SendVerificationCodeRequest ====================

    @Test
    @DisplayName("Should reject blank email in send verification code request / 发送验证码请求中空邮箱应被拒绝")
    void shouldRejectBlankEmailInSendVerificationCodeRequest() {
        SendVerificationCodeRequest request = SendVerificationCodeRequest.builder()
                .email("")
                .build();

        Set<ConstraintViolation<SendVerificationCodeRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("email");
    }

    @Test
    @DisplayName("Should reject invalid email format in send verification code request / 发送验证码请求中无效邮箱格式应被拒绝")
    void shouldRejectInvalidEmailFormatInSendVerificationCodeRequest() {
        SendVerificationCodeRequest request = SendVerificationCodeRequest.builder()
                .email("not-an-email")
                .build();

        Set<ConstraintViolation<SendVerificationCodeRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("email");
    }

    @Test
    @DisplayName("Should accept valid send verification code request / 合法发送验证码请求应被接受")
    void shouldAcceptValidSendVerificationCodeRequest() {
        SendVerificationCodeRequest request = SendVerificationCodeRequest.builder()
                .email("test@example.com")
                .build();

        Set<ConstraintViolation<SendVerificationCodeRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // ==================== CaptchaVerifyRequest ====================

    @Test
    @DisplayName("Should reject blank captchaId in verify request / 验证码校验中空captchaId应被拒绝")
    void shouldRejectBlankCaptchaIdInVerifyRequest() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest("", 42);

        Set<ConstraintViolation<CaptchaVerifyRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("captchaId");
    }

    @Test
    @DisplayName("Should reject null offset in verify request / 验证码校验中null偏移量应被拒绝")
    void shouldRejectNullOffsetInVerifyRequest() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest("challenge-token", null);

        Set<ConstraintViolation<CaptchaVerifyRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(fieldNames(violations)).contains("offsetX");
    }

    @Test
    @DisplayName("Should accept valid captcha verify request / 合法验证码校验请求应被接受")
    void shouldAcceptValidCaptchaVerifyRequest() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest("challenge-token", 42);

        Set<ConstraintViolation<CaptchaVerifyRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // ==================== Helper ====================

    private Set<String> fieldNames(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
