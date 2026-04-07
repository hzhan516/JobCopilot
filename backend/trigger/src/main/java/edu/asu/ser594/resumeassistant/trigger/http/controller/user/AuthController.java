package edu.asu.ser594.resumeassistant.trigger.http.controller.user;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;
import edu.asu.ser594.resumeassistant.api.user.facade.AuthFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;

    /**
     * 邮箱注册
     * Register by email
     */
    @PostMapping("/register/email")
    public ResponseEntity<ApiResponse<AuthResponse>> registerByEmail(
            @Valid @RequestBody RegisterByEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.registerByEmail(request)));
    }

    /**
     * 邮箱登录
     * Login by email
     */
    @PostMapping("/login/email")
    public ResponseEntity<ApiResponse<AuthResponse>> loginByEmail(
            @Valid @RequestBody LoginByEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.loginByEmail(request)));
    }
}