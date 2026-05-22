package io.jobcopilot.resumeassistant.api.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 邮箱登录请求DTO
 * Login by email request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginByEmailRequest {
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    private String password;

    private String captchaToken;
}
