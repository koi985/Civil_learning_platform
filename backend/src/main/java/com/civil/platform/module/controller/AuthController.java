package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.LoginRequest;
import com.civil.platform.module.dto.RegisterRequest;
import com.civil.platform.module.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    @OperationLog(module = "认证中心", name = "用户注册")
    public ApiResponse<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("注册成功", null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> currentUser() {
        return ApiResponse.success(authService.currentUser());
    }
}
