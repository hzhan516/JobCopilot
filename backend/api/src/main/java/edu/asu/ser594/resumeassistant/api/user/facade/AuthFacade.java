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
}
