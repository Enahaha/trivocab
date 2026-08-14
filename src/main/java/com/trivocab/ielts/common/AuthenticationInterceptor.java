package com.trivocab.ielts.common;

import com.trivocab.ielts.domain.UserRole;
import com.trivocab.ielts.exception.AuthenticationRequiredException;
import com.trivocab.ielts.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/system/heartbeat"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("POST".equalsIgnoreCase(request.getMethod()) && PUBLIC_POST_PATHS.contains(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(AuthSession.USER_ID) instanceof Number)) {
            throw new AuthenticationRequiredException("请先登录");
        }

        if (path.startsWith("/api/v1/admin/") || path.equals("/api/v1/admin")) {
            Object role = session.getAttribute(AuthSession.ROLE);
            if (!UserRole.ADMIN.name().equals(String.valueOf(role))) {
                throw new ForbiddenException("需要管理员权限");
            }
        }

        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            verifyCsrfToken(request, session);
        }
        return true;
    }

    private void verifyCsrfToken(HttpServletRequest request, HttpSession session) {
        String expected = (String) session.getAttribute(AuthSession.CSRF_TOKEN);
        String actual = request.getHeader(CSRF_HEADER);
        if (expected == null || actual == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ForbiddenException("CSRF 验证失败，请刷新页面后重试");
        }
    }
}
