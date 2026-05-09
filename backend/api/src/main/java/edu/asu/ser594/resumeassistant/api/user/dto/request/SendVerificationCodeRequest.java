package edu.asu.ser594.resumeassistant.api.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 发送验证码请求DTO
 * Send verification code request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendVerificationCodeRequest {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;
}
