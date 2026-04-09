package edu.asu.ser594.resumeassistant.infrastructure.security;

import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenServiceImpl Unit Tests
 * 
 * Tests the JWT token service implementation:
 * - Token generation
 * - Token validation
 * - User ID extraction
 * - Token expiration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Service Implementation Tests")
class JwtTokenServiceImplTest {

    private static final String TEST_SECRET = "myTestSecretKeyThatIsAtLeast32CharactersLongForHS256";
    private static final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days

    @InjectMocks
    private JwtTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        tokenService.init();
    }

    // ==================== Token Generation Tests ====================

    @Test
    @DisplayName("Should generate token pair with access and refresh tokens")
    void shouldGenerateTokenPairWithAccessAndRefreshTokens() {
        // When
        TokenPair result = tokenService.generateTokenPair(TEST_USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isNotNull().isNotEmpty();
        assertThat(result.getRefreshToken()).isNotNull().isNotEmpty();
        assertThat(result.getAccessToken()).isNotEqualTo(result.getRefreshToken());
    }

    @Test
    @DisplayName("Should set correct expiration time")
    void shouldSetCorrectExpirationTime() {
        // When
        TokenPair result = tokenService.generateTokenPair(TEST_USER_ID);

        // Then
        assertThat(result.getExpiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRATION / 1000);
    }

    @Test
    @DisplayName("Should generate different tokens for different calls")
    void shouldGenerateDifferentTokensForDifferentCalls() {
        // When
        TokenPair result1 = tokenService.generateTokenPair(TEST_USER_ID);
        TokenPair result2 = tokenService.generateTokenPair(TEST_USER_ID);

        // Then
        assertThat(result1.getAccessToken()).isNotEqualTo(result2.getAccessToken());
        assertThat(result1.getRefreshToken()).isNotEqualTo(result2.getRefreshToken());
    }

    @Test
    @DisplayName("Should generate valid tokens for different user IDs")
    void shouldGenerateValidTokensForDifferentUserIds() {
        // Given
        String userId1 = "11111111-1111-1111-1111-111111111111";
        String userId2 = "22222222-2222-2222-2222-222222222222";

        // When
        TokenPair result1 = tokenService.generateTokenPair(userId1);
        TokenPair result2 = tokenService.generateTokenPair(userId2);

        // Then
        assertThat(tokenService.validateToken(result1.getAccessToken())).isTrue();
        assertThat(tokenService.validateToken(result2.getAccessToken())).isTrue();
        assertThat(tokenService.getUserIdFromToken(result1.getAccessToken())).isEqualTo(userId1);
        assertThat(tokenService.getUserIdFromToken(result2.getAccessToken())).isEqualTo(userId2);
    }

    // ==================== Token Validation Tests ====================

    @Test
    @DisplayName("Should validate generated access token")
    void shouldValidateGeneratedAccessToken() {
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // When
        boolean isValid = tokenService.validateToken(tokenPair.getAccessToken());

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should validate generated refresh token")
    void shouldValidateGeneratedRefreshToken() {
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // When
        boolean isValid = tokenService.validateToken(tokenPair.getRefreshToken());

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate malformed token")
    void shouldInvalidateMalformedToken() {
        // Given
        String malformedToken = "not.a.valid.token";

        // When
        boolean isValid = tokenService.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate empty token")
    void shouldInvalidateEmptyToken() {
        // When
        boolean isValid = tokenService.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate null token")
    void shouldInvalidateNullToken() {
        // When
        boolean isValid = tokenService.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate tampered token")
    void shouldInvalidateTamperedToken() {
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);
        String tamperedToken = tokenPair.getAccessToken() + "tampered";

        // When
        boolean isValid = tokenService.validateToken(tamperedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    // ==================== User ID Extraction Tests ====================

    @Test
    @DisplayName("Should extract correct user ID from access token")
    void shouldExtractCorrectUserIdFromAccessToken() {
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // When
        String extractedUserId = tokenService.getUserIdFromToken(tokenPair.getAccessToken());

        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should extract correct user ID from refresh token")
    void shouldExtractCorrectUserIdFromRefreshToken() {
        // Given
        TokenPair tokenPair = tokenService.generateTokenPair(TEST_USER_ID);

        // When
        String extractedUserId = tokenService.getUserIdFromToken(tokenPair.getRefreshToken());

        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should extract different user IDs from different tokens")
    void shouldExtractDifferentUserIdsFromDifferentTokens() {
        // Given
        String userId1 = "11111111-1111-1111-1111-111111111111";
        String userId2 = "22222222-2222-2222-2222-222222222222";
        TokenPair tokenPair1 = tokenService.generateTokenPair(userId1);
        TokenPair tokenPair2 = tokenService.generateTokenPair(userId2);

        // When
        String extractedUserId1 = tokenService.getUserIdFromToken(tokenPair1.getAccessToken());
        String extractedUserId2 = tokenService.getUserIdFromToken(tokenPair2.getAccessToken());

        // Then
        assertThat(extractedUserId1).isEqualTo(userId1);
        assertThat(extractedUserId2).isEqualTo(userId2);
        assertThat(extractedUserId1).isNotEqualTo(extractedUserId2);
    }
}
