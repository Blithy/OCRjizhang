package com.example.ocrjizhang.backend.manage;

import com.example.ocrjizhang.backend.api.AuthPayloadDto;
import jakarta.servlet.http.HttpSession;

public final class ManageSession {

    public static final String ATTRIBUTE_USER_ID = "manage.userId";
    public static final String ATTRIBUTE_USERNAME = "manage.username";
    public static final String ATTRIBUTE_NICKNAME = "manage.nickname";
    public static final String ATTRIBUTE_TOKEN = "manage.token";

    private ManageSession() {
    }

    public static void signIn(HttpSession session, AuthPayloadDto payload) {
        session.setAttribute(ATTRIBUTE_USER_ID, payload.userId());
        session.setAttribute(ATTRIBUTE_USERNAME, payload.username());
        session.setAttribute(ATTRIBUTE_NICKNAME, payload.nickname());
        session.setAttribute(ATTRIBUTE_TOKEN, payload.token());
    }

    public static boolean isSignedIn(HttpSession session) {
        return session != null && session.getAttribute(ATTRIBUTE_USER_ID) instanceof Long;
    }

    public static long requireUserId(HttpSession session) {
        Object userId = session.getAttribute(ATTRIBUTE_USER_ID);
        if (userId instanceof Long value) {
            return value;
        }
        throw new IllegalStateException("后台会话未登录");
    }

    public static String displayName(HttpSession session) {
        Object nickname = session.getAttribute(ATTRIBUTE_NICKNAME);
        if (nickname instanceof String value && !value.isBlank()) {
            return "本机演示用户".equals(value) ? "本机用户" : value;
        }
        Object username = session.getAttribute(ATTRIBUTE_USERNAME);
        return username instanceof String value && !value.isBlank() ? value : "未登录用户";
    }

    public static String username(HttpSession session) {
        Object username = session.getAttribute(ATTRIBUTE_USERNAME);
        return username instanceof String value ? value : "";
    }

    public static String token(HttpSession session) {
        Object token = session.getAttribute(ATTRIBUTE_TOKEN);
        return token instanceof String value ? value : "";
    }
}
