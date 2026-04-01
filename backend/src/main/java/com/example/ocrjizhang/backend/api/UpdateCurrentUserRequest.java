package com.example.ocrjizhang.backend.api;

public record UpdateCurrentUserRequest(
    String nickname,
    String email,
    String phone,
    String password
) {
}
