package io.jobcopilot.resumeassistant.trigger.http.security;

import io.jobcopilot.resumeassistant.api.user.dto.TokenValidationResult;
import io.jobcopilot.resumeassistant.api.user.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 单元测试
 * JwtAuthenticationFilter Unit Tests
 * <p>
 * 测试 JWT 认证过滤器：
 * Tests the JWT authentication filter:
 * - 从请求头提取令牌
 * - Token extraction from header
 * - 令牌校验
 * - Token validation
 * - 认证上下文设置
 * - Authentication context setup
 * - 错误处理
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    private static final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String VALID_TOKEN = "valid.jwt.token";

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should skip filter for public endpoints")
    void shouldSkipFilterForPublicEndpoints() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/auth/login/email");

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        verify(tokenService, never()).validateToken(any());
    }

    @Test
    @DisplayName("Should skip filter for swagger endpoints")
    void shouldSkipFilterForSwaggerEndpoints() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        verify(tokenService, never()).validateToken(any());
    }

    @Test
    @DisplayName("Should skip filter for actuator endpoints")
    void shouldSkipFilterForActuatorEndpoints() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        verify(tokenService, never()).validateToken(any());
    }

    @Test
    @DisplayName("Should authenticate with valid token")
    void shouldAuthenticateWithValidToken() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenService.validateTokenDetailed(VALID_TOKEN)).thenReturn(TokenValidationResult.VALID);
        when(tokenService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should reject request without authorization header")
    void shouldRejectRequestWithoutAuthorizationHeader() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn(null);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should reject request with invalid authorization format")
    void shouldRejectRequestWithInvalidAuthorizationFormat() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should reject request with invalid token")
    void shouldRejectRequestWithInvalidToken() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
        when(tokenService.validateTokenDetailed("invalid_token")).thenReturn(TokenValidationResult.INVALID);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(stringWriter.toString()).contains("Invalid token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should handle case-insensitive bearer prefix")
    void shouldHandleCaseInsensitiveBearerPrefix() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("bearer " + VALID_TOKEN);
        when(tokenService.validateTokenDetailed(VALID_TOKEN)).thenReturn(TokenValidationResult.VALID);
        when(tokenService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract token correctly from header")
    void shouldExtractTokenCorrectlyFromHeader() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenService.validateTokenDetailed(VALID_TOKEN)).thenReturn(TokenValidationResult.VALID);
        when(tokenService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(tokenService).validateTokenDetailed(VALID_TOKEN);
        verify(tokenService).getUserIdFromToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("Should handle token extraction with extra spaces")
    void shouldHandleTokenExtractionWithExtraSpaces() throws ServletException, IOException {
        // 给定 - 过滤器提取带有空格的令牌
        // Given - filter extracts token with spaces
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("Bearer   " + VALID_TOKEN);
        // 过滤器将带有额外空格的令牌传递给 validateTokenDetailed
        // The filter passes the token WITH the extra spaces to validateTokenDetailed
        when(tokenService.validateTokenDetailed("  " + VALID_TOKEN)).thenReturn(TokenValidationResult.VALID);
        when(tokenService.getUserIdFromToken("  " + VALID_TOKEN)).thenReturn(TEST_USER_ID);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(tokenService).validateTokenDetailed("  " + VALID_TOKEN);
    }

    @Test
    @DisplayName("Should return 401 for expired token")
    void shouldReturn401ForExpiredToken() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("Bearer expired_token");
        when(tokenService.validateTokenDetailed("expired_token")).thenReturn(TokenValidationResult.EXPIRED);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(stringWriter.toString()).contains("Token expired");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should set ADMIN role authority when JWT contains ADMIN role claim")
    void shouldSetAdminRoleAuthority() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/api/admin/v1/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenService.validateTokenDetailed(VALID_TOKEN)).thenReturn(TokenValidationResult.VALID);
        when(tokenService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(tokenService.getRoleFromToken(VALID_TOKEN)).thenReturn("ADMIN");

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities().stream().map(Object::toString).toList())
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should set empty authorities when no role claim in JWT")
    void shouldSetEmptyAuthoritiesWhenNoRoleClaim() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenService.validateTokenDetailed(VALID_TOKEN)).thenReturn(TokenValidationResult.VALID);
        when(tokenService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(tokenService.getRoleFromToken(VALID_TOKEN)).thenReturn(null);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Should set JOB_SEEKER role authority for non-admin users")
    void shouldSetJobSeekerRoleAuthority() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenService.validateTokenDetailed(VALID_TOKEN)).thenReturn(TokenValidationResult.VALID);
        when(tokenService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(tokenService.getRoleFromToken(VALID_TOKEN)).thenReturn("JOB_SEEKER");

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities().stream().map(Object::toString).toList())
                .containsExactly("ROLE_JOB_SEEKER");
    }

    @Test
    @DisplayName("Should continue chain when no authorization header")
    void shouldContinueChainWhenNoAuthorizationHeader() throws ServletException, IOException {
        // 给定
        // Given
        when(request.getRequestURI()).thenReturn("/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn(null);

        // 当
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // 那么
        // Then
        verify(filterChain).doFilter(request, response);
    }
}
