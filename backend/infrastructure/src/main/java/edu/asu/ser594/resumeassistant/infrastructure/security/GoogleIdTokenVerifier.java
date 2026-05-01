package edu.asu.ser594.resumeassistant.infrastructure.security;

import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Google ID Token 验证器
 * Google ID Token verifier
 *
 * 调用 Google tokeninfo 端点验证 ID Token 的真实性
 * Calls Google tokeninfo endpoint to verify ID Token authenticity
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleIdTokenVerifier {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";

    private final RestTemplate restTemplate;

    /**
     * 验证 Google ID Token
     * Verify Google ID Token
     *
     * @param idToken Google ID Token
     * @return 解析后的 Google 用户信息
     */
    public GoogleUserInfo verify(String idToken) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    GOOGLE_TOKENINFO_URL,
                    Map.class,
                    idToken
            );

            if (response == null || response.get("email") == null) {
                log.warn("Google tokeninfo returned empty or invalid response");
                throw new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS);
            }

            // 检查错误字段（无效令牌）
            // Check for error field (invalid token)
            if (response.containsKey("error")) {
                log.warn("Google tokeninfo returned error: {}", response.get("error"));
                throw new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS);
            }

            String email = (String) response.get("email");
            String providerUserId = (String) response.get("sub");
            String displayName = (String) response.get("name");
            String avatarUrl = (String) response.get("picture");
            Boolean emailVerified = Boolean.parseBoolean(String.valueOf(response.get("email_verified")));

            return new GoogleUserInfo(email, providerUserId, displayName, avatarUrl, emailVerified);

        } catch (RestClientException e) {
            log.warn("Failed to verify Google ID Token: {}", e.getMessage());
            throw new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS);
        }
    }

    /**
     * Google 用户信息
     * Google user info
     */
    public record GoogleUserInfo(
            String email,
            String providerUserId,
            String displayName,
            String avatarUrl,
            boolean emailVerified
    ) {}
}
