package com.example.ocrjizhang.backend.api;

public record RegisterRequest(
    String username,
    String password,
    String email,
    String phone,
    String nickname
) {
}
