package io.jobcopilot.resumeassistant.trigger.http.security;

import io.jobcopilot.resumeassistant.api.user.dto.TokenValidationResult;
import io.jobcopilot.resumeassistant.api.user.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT filter that extracts bearer tokens and establishes the Spring Security context
 * for every incoming request without server-side session storage.
 * 无状态 JWT 过滤器，从每个请求中提取 Bearer 令牌并建立 Spring Security 上下文，无需服务端会话存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        TokenValidationResult result = tokenService.validateTokenDetailed(token);

        switch (result) {
            case VALID -> {
                String userId = tokenService.getUserIdFromToken(token);
                String role = tokenService.getRoleFromToken(token);

                // propagate user identity to downstream resolvers via request attributes | 通过请求属性将用户身份传递给下游解析器
                request.setAttribute("userId", userId);

                // bridge JWT to Spring Security context with role-based authority for RBAC
                // 将 JWT 桥接到 Spring Security 上下文，附带角色权限以支持 RBAC
                var authorities = role != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        : List.<SimpleGrantedAuthority>of();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT authentication successful for user: {}, role: {}", userId, role);
                filterChain.doFilter(request, response);
            }
            case EXPIRED -> {
                log.warn("JWT token expired for request: {}", request.getRequestURI());
                writeUnauthorizedResponse(response, "Token expired / Token 已过期");
            }
            case INVALID -> {
                log.warn("Invalid JWT token for request: {}", request.getRequestURI());
                writeUnauthorizedResponse(response, "Invalid token / Token 无效");
            }
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":401,\"message\":\"%s\"}", message));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
