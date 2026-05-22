package io.jobcopilot.resumeassistant.infrastructure.security;

import io.jobcopilot.resumeassistant.api.user.dto.TokenPair;
import io.jobcopilot.resumeassistant.api.user.dto.TokenValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenServiceImpl 单元测试
 * JwtTokenServiceImpl Unit Tests
 * <p>
 * 测试 JWT 令牌服务实现：
 * Tests the JWT token service implementation:
 * - 令牌生成
 * - Token generation
 * - 令牌验证
 * - Token validation
 * - 用户 ID 提取
 * - User ID extraction
 * - 令牌过期
 * - Token expiration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Service Implementation Tests")
class JwtTokenServiceImplTest {

    private static final String TEST_SECRET = "myTestSecretKeyThatIsAtLeast32CharactersLongForHS256";
    private static final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000; // 1 hour
    // 1 小时
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days
    // 7 天

    @InjectMocks
    private JwtTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        tokenService.init();
    }

    // ==================== 令牌生成测试 ====================
    // ==================== Token Generation Tests ====================

    @Test
    @DisplayName("Should generate token pair with access and refresh tokens")
    void shouldGenerateTokenPairWithAccessAndRefreshTokens() {
        // 当
        // When
        TokenPair result = tokenService.generateTokenPair(TEST_USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isNotNull().isNotEmpty();
        assertThat(result.getRefreshToken()).isNotNull().isNotEmpty();
        assertThat(result.getAccessToken()).isNotEqualTo(result.getRefreshToken());
    }

    @Test
    @DisplayName("Should set correct expiration time")
    void shouldSetCorrectExpirationTime() {
        // 当
        // When
        TokenPair result = tokenService.generateTokenPair(TEST_USER_ID);

        // 然后
        // Then
        assertThat(result.getExpiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRATION / 1000);
    }

    @Test
    @DisplayName("Should generate different tokens for different calls")
    void shouldGenerateDifferentTokensForDifferentCalls() throws InterruptedException {
        // 当
        // When
        TokenPair result1 = tokenService.generateTokenPair(TEST_USER_ID);
        Thread.sleep(1000);
        TokenPair result2 = tokenService.generateTokenPair(TEST_USER_ID);

        // 然后
        // Then
        assertThat(result1.getAccessToken()).isNotEqualTo(result2.getAccessToken());
        assertThat(result1.getRefreshToken()).isNotEqualTo(result2.getRefreshToken());
    }

    @Test
    @DisplayName("Should generate valid tokens for different user IDs")
    void shouldGenerateValidTokensForDifferentUserIds() {
        // 给定
        // Given
        String userId1 = "11111111-1111-1111-1111-111111111111";
        String userId2 = "22222222-2222-2222-2222-222222222222";

        // 当
        // When
        TokenPair result1 = tokenService.generateTokenPair(userId1);
        TokenPair result2 = tokenService.generateTokenPair(userId2);

        // 然后
        // Then
        assertThat(tokenService.validateToken(result1.getAccessToken())).isTrue();
        assertThat(tokenService.validateToken(result2.getAccessToken())).isTrue();
        assertThat(tokenService.getUserIdFromToken(result1.getAccessToken())).isEqualTo(userId1);
        assertThat(tokenService.getUserIdFromToken(result2.getAccessToken())).isEqualTo(userId2);
    }

    // ==================== 令牌验证测试 ====================
    // ==================== Token Validation Tests ====================

    @Test
    @DisplayName("Should validate generated access token")
    void shouldValidateGeneratedAccessToken() {
        // 给定
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // 当
        // When
        boolean isValid = tokenService.validateToken(tokenPair.getAccessToken());

        // 然后
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should validate generated refresh token")
    void shouldValidateGeneratedRefreshToken() {
        // 给定
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // 当
        // When
        boolean isValid = tokenService.validateToken(tokenPair.getRefreshToken());

        // 然后
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate malformed token")
    void shouldInvalidateMalformedToken() {
        // 给定
        // Given
        String malformedToken = "not.a.valid.token";

        // 当
        // When
        boolean isValid = tokenService.validateToken(malformedToken);

        // 然后
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate empty token")
    void shouldInvalidateEmptyToken() {
        // 当
        // When
        boolean isValid = tokenService.validateToken("");

        // 然后
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate null token")
    void shouldInvalidateNullToken() {
        // 当
        // When
        boolean isValid = tokenService.validateToken(null);

        // 然后
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate tampered token")
    void shouldInvalidateTamperedToken() {
        // 给定
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);
        String tamperedToken = tokenPair.getAccessToken() + "tampered";

        // 当
        // When
        boolean isValid = tokenService.validateToken(tamperedToken);

        // 然后
        // Then
        assertThat(isValid).isFalse();
    }

    // ==================== 用户 ID 提取测试 ====================
    // ==================== User ID Extraction Tests ====================

    @Test
    @DisplayName("Should extract correct user ID from access token")
    void shouldExtractCorrectUserIdFromAccessToken() {
        // 给定
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // 当
        // When
        String extractedUserId = tokenService.getUserIdFromToken(tokenPair.getAccessToken());

        // 然后
        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should extract correct user ID from refresh token")
    void shouldExtractCorrectUserIdFromRefreshToken() {
        // 给定
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // 当
        // When
        String extractedUserId = tokenService.getUserIdFromToken(tokenPair.getRefreshToken());

        // 然后
        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should extract different user IDs from different tokens")
    void shouldExtractDifferentUserIdsFromDifferentTokens() {
        // 给定
        // Given
        String userId1 = "11111111-1111-1111-1111-111111111111";
        String userId2 = "22222222-2222-2222-2222-222222222222";
        TokenPair tokenPair1 = tokenService.generateTokenPair(userId1);
        TokenPair tokenPair2 = tokenService.generateTokenPair(userId2);

        // 当
        // When
        String extractedUserId1 = tokenService.getUserIdFromToken(tokenPair1.getAccessToken());
        String extractedUserId2 = tokenService.getUserIdFromToken(tokenPair2.getAccessToken());

        // 然后
        // Then
        assertThat(extractedUserId1).isEqualTo(userId1);
        assertThat(extractedUserId2).isEqualTo(userId2);
        assertThat(extractedUserId1).isNotEqualTo(extractedUserId2);
    }

    // ==================== 详细校验测试 ====================
    // ==================== Detailed Validation Tests ====================

    @Test
    @DisplayName("Should return VALID for valid token via detailed validation")
    void shouldReturnValidForValidTokenViaDetailedValidation() {
        // 给定
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // 当
        // When
        TokenValidationResult result = tokenService.validateTokenDetailed(tokenPair.getAccessToken());

        // 然后
        // Then
        assertThat(result).isEqualTo(TokenValidationResult.VALID);
    }

    @Test
    @DisplayName("Should return EXPIRED for expired token via detailed validation")
    void shouldReturnExpiredForExpiredTokenViaDetailedValidation() throws InterruptedException {
        // 给定
        // Given
        ReflectionTestUtils.setField(tokenService, "accessTokenExpiration", 1); // 1 ms
        tokenService.init();
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);
        Thread.sleep(10); // ensure expiration

        // 当
        // When
        TokenValidationResult result = tokenService.validateTokenDetailed(tokenPair.getAccessToken());

        // 然后
        // Then
        assertThat(result).isEqualTo(TokenValidationResult.EXPIRED);
    }

    @Test
    @DisplayName("Should return INVALID for malformed token via detailed validation")
    void shouldReturnInvalidForMalformedTokenViaDetailedValidation() {
        // 给定
        // Given
        String malformedToken = "not.a.valid.token";

        // 当
        // When
        TokenValidationResult result = tokenService.validateTokenDetailed(malformedToken);

        // 然后
        // Then
        assertThat(result).isEqualTo(TokenValidationResult.INVALID);
    }
}
