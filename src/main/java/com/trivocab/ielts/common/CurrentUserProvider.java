package com.trivocab.ielts.common;

import com.trivocab.ielts.domain.UserRole;
import com.trivocab.ielts.exception.AuthenticationRequiredException;
import com.trivocab.ielts.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CurrentUserProvider {
    private final long demoUserId;
    private final boolean allowDemoUser;

    public CurrentUserProvider(
            @Value("${app.demo-user-id:1}") long demoUserId,
            @Value("${app.auth.allow-demo-user:false}") boolean allowDemoUser
    ) {
        this.demoUserId = demoUserId;
        this.allowDemoUser = allowDemoUser;
    }

    public long userId() {
        HttpSession session = currentSession();
        if (session != null) {
            Object value = session.getAttribute(AuthSession.USER_ID);
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        if (allowDemoUser) {
            return demoUserId;
        }
        throw new AuthenticationRequiredException("请先登录");
    }

    public UserRole role() {
        HttpSession session = currentSession();
        if (session != null) {
            Object value = session.getAttribute(AuthSession.ROLE);
            if (value != null) {
                try {
                    return UserRole.valueOf(value.toString());
                } catch (IllegalArgumentException ignored) {
                    // Invalid/stale sessions are treated as unauthenticated below.
                }
            }
        }
        if (allowDemoUser) {
            return UserRole.USER;
        }
        throw new AuthenticationRequiredException("请先登录");
    }

    public void requireAdmin() {
        if (role() != UserRole.ADMIN) {
            throw new ForbiddenException("需要管理员权限");
        }
    }

    private HttpSession currentSession() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getSession(false);
    }
}
