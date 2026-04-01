package com.example.ocrjizhang.backend.controller;

import com.example.ocrjizhang.backend.api.ApiResponse;
import com.example.ocrjizhang.backend.api.AuthPayloadDto;
import com.example.ocrjizhang.backend.api.UpdateCurrentUserRequest;
import com.example.ocrjizhang.backend.auth.RequestUser;
import com.example.ocrjizhang.backend.store.DemoStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final DemoStore demoStore;

    public UserController(DemoStore demoStore) {
        this.demoStore = demoStore;
    }

    @GetMapping("/me")
    public ApiResponse<AuthPayloadDto> getCurrentUser(HttpServletRequest request) {
        return ApiResponse.success(demoStore.getCurrentUser(RequestUser.requireUserId(request)));
    }

    @PutMapping("/me")
    public ApiResponse<AuthPayloadDto> updateCurrentUser(
        HttpServletRequest request,
        @RequestBody UpdateCurrentUserRequest updateRequest
    ) {
        return ApiResponse.success(demoStore.updateCurrentUser(RequestUser.requireUserId(request), updateRequest));
    }
}
