package io.jobcopilot.resumeassistant.infrastructure.security;

import io.jobcopilot.resumeassistant.api.user.dto.TokenPair;
import io.jobcopilot.resumeassistant.api.user.dto.TokenValidationResult;
import io.jobcopilot.resumeassistant.api.user.service.TokenService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token generation and validation using JJWT with HMAC-SHA.
 * Token pairs support sliding-session refresh: short-lived access tokens minimize
 * exposure window, while refresh tokens allow seamless re-authentication.
 * 基于 JJWT HMAC-SHA 的令牌生成与校验；采用双令牌滑动会话机制，短期访问令牌降低暴露风险，刷新令牌实现无感续登
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements TokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 characters long. "
                            + "Please set a strong jwt.secret in your configuration. / "
                            + "JWT 密钥长度必须至少为 32 个字符，请在配置中设置强密钥。"
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public TokenPair generateTokenPair(String userId) {
        String accessToken = generateToken(userId, accessTokenExpiration);
        String refreshToken = generateToken(userId, refreshTokenExpiration);

        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }

    private String generateToken(String userId, long expiration) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String getUserIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token format: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public TokenValidationResult validateTokenDetailed(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return TokenValidationResult.EXPIRED;
        } catch (Exception e) {
            log.warn("Invalid JWT token signature or claims: {}", e.getMessage());
            return TokenValidationResult.INVALID;
        }
    }
}
