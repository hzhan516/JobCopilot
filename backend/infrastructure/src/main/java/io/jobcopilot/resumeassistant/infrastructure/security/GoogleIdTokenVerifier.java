package io.jobcopilot.resumeassistant.infrastructure.security;

import io.jobcopilot.resumeassistant.domain.user.exception.AuthException;
import io.jobcopilot.resumeassistant.domain.user.port.GoogleTokenVerifierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Verifies Google ID Tokens by calling the Google tokeninfo endpoint.
 * This is a lightweight alternative to the full Google API client library, trading
 * rich feature support for a smaller dependency footprint in the backend.
 * 通过调用 Google tokeninfo 端点验证 ID Token；相比完整 Google API 客户端库，以较小的依赖体积换取核心验证能力
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleIdTokenVerifier implements GoogleTokenVerifierPort {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";

    private final RestTemplate restTemplate;

    @Override
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

            if (response.containsKey("error")) {
                log.warn("Google tokeninfo returned error: {}", response.get("error"));
                throw new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS);
            }

            String email = (String) response.get("email");
            String providerUserId = (String) response.get("sub");
            String displayName = (String) response.get("name");
            String avatarUrl = (String) response.get("picture");
            boolean emailVerified = Boolean.parseBoolean(String.valueOf(response.get("email_verified")));

            return new GoogleUserInfo(email, providerUserId, displayName, avatarUrl, emailVerified);

        } catch (RestClientException e) {
            log.warn("Failed to verify Google ID Token: {}", e.getMessage());
            throw new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS);
        }
    }
}
