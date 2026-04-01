package com.example.ocrjizhang.backend.auth;

import com.example.ocrjizhang.backend.api.ApiResponse;
import com.example.ocrjizhang.backend.store.DemoStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final DemoStore demoStore;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(DemoStore demoStore, ObjectMapper objectMapper) {
        this.demoStore = demoStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "Missing token");
            return false;
        }

        Long userId = demoStore.findUserIdByToken(header.substring(BEARER_PREFIX.length()));
        if (userId == null) {
            writeUnauthorized(response, "Token invalid");
            return false;
        }

        request.setAttribute(RequestUser.ATTRIBUTE_USER_ID, userId);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(401, message));
    }
}
