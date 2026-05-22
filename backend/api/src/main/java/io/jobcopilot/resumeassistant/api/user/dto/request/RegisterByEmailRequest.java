package io.jobcopilot.resumeassistant.api.user.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 邮箱注册请求DTO
 * Register by email request DTO
 * <p>
 * 注意：@Builder 与 @NoArgsConstructor/@AllArgsConstructor 配合使用时
 * 需要显式声明构造函数注解，否则 Jackson 反序列化可能失败
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterByEmailRequest {
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, max = 32, message = "{validation.password.length}")
    private String password;

    private String verificationCode;

    private String captchaToken;
}
