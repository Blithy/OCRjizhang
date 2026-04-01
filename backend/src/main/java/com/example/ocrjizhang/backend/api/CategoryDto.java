package com.example.ocrjizhang.backend.api;

public record CategoryDto(
    long id,
    long userId,
    String name,
    String type,
    String icon,
    String color,
    boolean isDefault,
    long createdAt,
    long updatedAt
) {
}
