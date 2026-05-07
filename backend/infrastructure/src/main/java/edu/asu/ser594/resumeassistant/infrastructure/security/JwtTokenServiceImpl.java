package edu.asu.ser594.resumeassistant.infrastructure.security;

import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import edu.asu.ser594.resumeassistant.api.user.dto.TokenValidationResult;
import edu.asu.ser594.resumeassistant.api.user.service.TokenService;
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
 * JWT 令牌服务实现 / JWT token service implementation
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

    /**
     * 初始化密钥 / Initialize secret key
     */
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌和刷新令牌对 / Generate access and refresh token pair
     */
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

    // 生成单个令牌 / Generate a single token
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

    /**
     * 从令牌中提取用户 ID / Extract user ID from token
     */
    @Override
    public String getUserIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * 验证令牌有效性 / Validate token
     */
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

    /**
     * 详细校验令牌，区分过期与无效
     * Validate token with detailed result, distinguishing expired from invalid
     */
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
