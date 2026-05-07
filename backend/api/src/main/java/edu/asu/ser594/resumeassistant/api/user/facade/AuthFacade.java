package edu.asu.ser594.resumeassistant.api.user.facade;

import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByGoogleRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;

/**
 * Authentication facade abstracting identity providers (local email vs OAuth) behind a uniform contract.
 * 认证门面，将本地邮箱与 OAuth 等身份源抽象为统一契约，隔离 Trigger 层与具体认证策略
 */
public interface AuthFacade {
    AuthResponse registerByEmail(RegisterByEmailRequest request);

    AuthResponse loginByEmail(LoginByEmailRequest request);

    AuthResponse loginByGoogle(LoginByGoogleRequest request);

    /**
     * Issues a new token pair without requiring credentials, provided the refresh token is still valid.
     * 在刷新令牌仍有效的前提下，无需重新提供凭证即可签发新的令牌对，实现无感续期
     *
     * @param refreshToken long-lived refresh token / 长期有效的刷新令牌
     * @return new access and refresh tokens / 新的访问令牌与刷新令牌
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Revokes the given access token on the application side; stateless JWTs remain technically valid until expiry.
     * 在应用层注销指定访问令牌；由于 JWT 本身无状态，令牌在技术层面仍有效直至过期
     *
     * @param accessToken the token to revoke / 待注销的访问令牌
     */
    void logout(String accessToken);
}
