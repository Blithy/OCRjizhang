package com.example.ocrjizhang.backend.api;

public record AuthPayloadDto(
    String token,
    long userId,
    String username,
    String nickname
) {
}
