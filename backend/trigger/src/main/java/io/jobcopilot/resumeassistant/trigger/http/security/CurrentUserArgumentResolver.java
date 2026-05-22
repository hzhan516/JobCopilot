package io.jobcopilot.resumeassistant.trigger.http.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * Bridges JWT-authenticated user identity into controller method parameters so endpoints can declare
 * the current user as a typed method argument rather than manually inspecting the request.
 * 将 JWT 认证后的用户身份桥接到控制器方法参数，使端点能够以类型化参数声明当前用户，无需手动解析请求
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(@NotNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        // userId is injected by JwtAuthenticationFilter ahead of controller invocation | userId 由 JwtAuthenticationFilter 在控制器调用前注入
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        Object userId = request.getAttribute("userId");
        return userId != null ? UUID.fromString(userId.toString()) : null;
    }
}
