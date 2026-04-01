package com.example.ocrjizhang.backend.auth;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUser {

    public static final String ATTRIBUTE_USER_ID = "currentUserId";

    private RequestUser() {
    }

    public static long requireUserId(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE_USER_ID);
        if (value instanceof Long userId) {
            return userId;
        }
        throw new IllegalStateException("Missing authenticated user");
    }
}
