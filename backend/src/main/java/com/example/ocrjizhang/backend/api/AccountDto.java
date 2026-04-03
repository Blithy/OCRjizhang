package com.example.ocrjizhang.backend.api;

public record AccountDto(
    long id,
    long userId,
    String name,
    String symbol,
    long balanceFen,
    boolean isDefault,
    long createdAt,
    long updatedAt
) {
}
