package edu.asu.ser594.resumeassistant.api.user.facade;

import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByGoogleRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;

// 认证门面接口
// Authentication facade interface
public interface AuthFacade {
    // 邮箱注册
    // Register by email
    AuthResponse registerByEmail(RegisterByEmailRequest request);

    // 邮箱登录
    // Login by email
    AuthResponse loginByEmail(LoginByEmailRequest request);

    // Google 登录
    // Login by Google
    AuthResponse loginByGoogle(LoginByGoogleRequest request);

    /**
     * 刷新访问令牌
     * Refresh access token
     *
     * @param refreshToken 刷新令牌 / Refresh token
     * @return 新认证响应 / New authentication response
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * 用户注销
     * User logout
     *
     * @param accessToken 当前访问令牌 / Current access token
     */
    void logout(String accessToken);
}
