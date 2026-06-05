package io.jobcopilot.resumeassistant.infrastructure.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 内部 API Key 拦截器测试 / Internal API key interceptor tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Internal API Key Interceptor Tests / 内部 API Key 拦截器测试")
class InternalApiKeyInterceptorTest {

    private InternalApiKeyInterceptor createInterceptor(String apiKey) throws Exception {
        InternalApiKeyInterceptor interceptor = new InternalApiKeyInterceptor();
        Field apiKeyField = InternalApiKeyInterceptor.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(interceptor, apiKey);
        return interceptor;
    }

    @Test
    @DisplayName("Should add internal API key header when key is configured / 当密钥已配置时应添加内部 API Key header")
    void intercept_WhenApiKeyConfigured_ShouldAddHeader() throws Exception {
        // Given
        InternalApiKeyInterceptor interceptor = createInterceptor("test-key-123");
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse = mock(ClientHttpResponse.class);
        var headers = new org.springframework.http.HttpHeaders();

        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(expectedResponse);

        // When
        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        assertThat(headers.getFirst("X-Internal-API-Key")).isEqualTo("test-key-123");
    }

    @Test
    @DisplayName("Should not add header when API key is null / 当 API Key 为 null 时不应添加 header")
    void intercept_WhenApiKeyNull_ShouldNotAddHeader() throws Exception {
        // Given
        InternalApiKeyInterceptor interceptor = createInterceptor(null);
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse = mock(ClientHttpResponse.class);
        var headers = new org.springframework.http.HttpHeaders();

        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(expectedResponse);

        // When
        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        assertThat(headers.containsKey("X-Internal-API-Key")).isFalse();
    }

    @Test
    @DisplayName("Should not add header when API key is blank / 当 API Key 为空字符串时不应添加 header")
    void intercept_WhenApiKeyBlank_ShouldNotAddHeader() throws Exception {
        // Given
        InternalApiKeyInterceptor interceptor = createInterceptor("   ");
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse = mock(ClientHttpResponse.class);
        var headers = new org.springframework.http.HttpHeaders();

        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(expectedResponse);

        // When
        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        assertThat(headers.containsKey("X-Internal-API-Key")).isFalse();
    }

    @Test
    @DisplayName("Should pass through execution chain / 应继续执行链")
    void intercept_ShouldPassThroughExecutionChain() throws Exception {
        // Given
        InternalApiKeyInterceptor interceptor = createInterceptor("key");
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse = mock(ClientHttpResponse.class);
        var headers = new org.springframework.http.HttpHeaders();

        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(expectedResponse);

        // When
        ClientHttpResponse response = interceptor.intercept(request, new byte[]{1, 2, 3}, execution);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(execution).execute(any(HttpRequest.class), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).containsExactly(1, 2, 3);
    }
}
