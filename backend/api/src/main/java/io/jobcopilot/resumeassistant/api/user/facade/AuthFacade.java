package io.jobcopilot.resumeassistant.api.user.facade;

import io.jobcopilot.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.LoginByGoogleRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.SendVerificationCodeRequest;
import io.jobcopilot.resumeassistant.api.user.dto.response.AuthResult;

/**
 * Authentication facade abstracting identity providers (local email vs OAuth) behind a uniform contract.
 * 认证门面，将本地邮箱与 OAuth 等身份源抽象为统一契约，隔离 Trigger 层与具体认证策略
 */
public interface AuthFacade {
    AuthResult registerByEmail(RegisterByEmailRequest request);

    AuthResult loginByEmail(LoginByEmailRequest request);

    AuthResult loginByGoogle(LoginByGoogleRequest request);

    /**
     * Issues a new token pair without requiring credentials, provided the refresh token is still valid.
     *
     * @param refreshToken long-lived refresh token
     * @return new access token (body) + new refresh token (for HttpOnly cookie)
     */
    AuthResult refreshToken(String refreshToken);

    /**
     * Revokes the given access token on the application side; stateless JWTs remain technically valid until expiry.
     *
     * @param accessToken the token to revoke
     */
    void logout(String accessToken);

    /**
     * 发送邮箱验证码
     */
    void sendVerificationCode(SendVerificationCodeRequest request);

    /**
     * 查询邮箱验证功能是否开启
     */
    boolean isEmailVerificationEnabled();
}
