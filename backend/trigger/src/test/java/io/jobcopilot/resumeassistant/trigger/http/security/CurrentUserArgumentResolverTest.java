package io.jobcopilot.resumeassistant.trigger.http.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 当前用户参数解析器测试 / Current user argument resolver tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Current User Argument Resolver Tests / 当前用户参数解析器测试")
class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @Test
    @DisplayName("Should support parameter with @CurrentUser and UUID type / 应支持带有 @CurrentUser 注解且类型为 UUID 的参数")
    void supportsParameter_WithCurrentUserAndUUID_ShouldReturnTrue() {
        // Given
        MethodParameter parameter = createMethodParameter(CurrentUser.class, UUID.class);

        // When
        boolean result = resolver.supportsParameter(parameter);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not support parameter without @CurrentUser / 不应支持没有 @CurrentUser 注解的参数")
    void supportsParameter_WithoutCurrentUser_ShouldReturnFalse() {
        // Given
        MethodParameter parameter = createMethodParameter(null, UUID.class);

        // When
        boolean result = resolver.supportsParameter(parameter);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should not support non-UUID parameter / 不应支持非 UUID 类型的参数")
    void supportsParameter_WithNonUUIDType_ShouldReturnFalse() {
        // Given
        MethodParameter parameter = createMethodParameter(CurrentUser.class, String.class);

        // When
        boolean result = resolver.supportsParameter(parameter);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should resolve UUID from request attribute / 应从请求属性中解析 UUID")
    void resolveArgument_WithUserIdAttribute_ShouldReturnUUID() throws Exception {
        // Given
        UUID expectedUserId = UUID.randomUUID();
        HttpServletRequest request = mock(HttpServletRequest.class);
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest()).thenReturn(request);
        when(request.getAttribute("userId")).thenReturn(expectedUserId.toString());

        MethodParameter parameter = createMethodParameter(CurrentUser.class, UUID.class);

        // When
        Object result = resolver.resolveArgument(parameter, null, webRequest, null);

        // Then
        assertThat(result).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("Should return null when userId attribute is missing / 当 userId 属性不存在时应返回 null")
    void resolveArgument_WithoutUserIdAttribute_ShouldReturnNull() throws Exception {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest()).thenReturn(request);
        when(request.getAttribute("userId")).thenReturn(null);

        MethodParameter parameter = createMethodParameter(CurrentUser.class, UUID.class);

        // When
        Object result = resolver.resolveArgument(parameter, null, webRequest, null);

        // Then
        assertThat(result).isNull();
    }

    // Helper to create a mock MethodParameter
    private MethodParameter createMethodParameter(Class<? extends Annotation> annotationType, Class<?> paramType) {
        // We use a simplified approach: create a MethodParameter that returns our desired values
        return new MethodParameter(MockMethod.class.getMethods()[0], 0) {
            @Override
            public boolean hasParameterAnnotation(Class<? extends Annotation> annotationType2) {
                return annotationType != null && annotationType.equals(annotationType2);
            }

            @Override
            public Class<?> getParameterType() {
                return paramType;
            }
        };
    }

    // Dummy class for MethodParameter creation
    private static class MockMethod {
        public void dummy(UUID param) {}
    }
}
