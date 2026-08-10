package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.dto.AuthMeResponse;
import com.trivocab.ielts.dto.ForgotPasswordRequest;
import com.trivocab.ielts.dto.ForgotPasswordResponse;
import com.trivocab.ielts.dto.LoginRequest;
import com.trivocab.ielts.dto.RegisterRequest;
import com.trivocab.ielts.dto.ResetPasswordRequest;
import com.trivocab.ielts.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthMeResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(request, httpRequest), "注册成功"));
    }

    @PostMapping("/login")
    public ApiResponse<AuthMeResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(authService.login(request, httpRequest), "登录成功");
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me(HttpServletRequest request) {
        return ApiResponse.ok(authService.me(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponse.ok(null, "已退出登录");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ApiResponse.ok(
                authService.forgotPassword(request),
                "如果该邮箱已注册，重置验证码已生成"
        );
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.resetPassword(request, httpRequest);
        return ApiResponse.ok(null, "密码已重置，请使用新密码登录");
    }
}
