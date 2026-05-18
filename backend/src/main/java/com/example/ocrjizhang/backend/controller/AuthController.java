package com.example.ocrjizhang.backend.controller;

import com.example.ocrjizhang.backend.api.ApiResponse;
import com.example.ocrjizhang.backend.api.AuthPayloadDto;
import com.example.ocrjizhang.backend.api.LoginRequest;
import com.example.ocrjizhang.backend.api.RegisterRequest;
import com.example.ocrjizhang.backend.store.DemoStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口控制器。
 * 提供注册和登录接口，供 Android 客户端和后台管理页共用。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DemoStore demoStore;

    public AuthController(DemoStore demoStore) {
        this.demoStore = demoStore;
    }

    @PostMapping("/register")
    public ApiResponse<AuthPayloadDto> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(demoStore.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthPayloadDto> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(demoStore.login(request));
    }
}
