package com.trivocab.ielts.service;

import com.trivocab.ielts.common.AuthSession;
import com.trivocab.ielts.domain.PasswordResetTokenRow;
import com.trivocab.ielts.domain.UserAccountRow;
import com.trivocab.ielts.domain.UserRole;
import com.trivocab.ielts.dto.AuthMeResponse;
import com.trivocab.ielts.dto.ForgotPasswordRequest;
import com.trivocab.ielts.dto.ForgotPasswordResponse;
import com.trivocab.ielts.dto.LoginRequest;
import com.trivocab.ielts.dto.RegisterRequest;
import com.trivocab.ielts.dto.ResetPasswordRequest;
import com.trivocab.ielts.exception.AuthenticationRequiredException;
import com.trivocab.ielts.exception.ConflictException;
import com.trivocab.ielts.exception.InvalidResetCodeException;
import com.trivocab.ielts.mapper.AuthMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_BCRYPT_BYTES = 72;

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final boolean exposeResetCode;
    private final boolean allowShutdown;
    private final long resetCodeTtlMinutes;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String dummyPasswordHash;

    public AuthService(
            AuthMapper authMapper,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${app.auth.expose-reset-code:false}") boolean exposeResetCode,
            @Value("${app.allow-shutdown:false}") boolean allowShutdown,
            @Value("${app.auth.reset-code-ttl-minutes:10}") long resetCodeTtlMinutes
    ) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.exposeResetCode = exposeResetCode;
        this.allowShutdown = allowShutdown;
        this.resetCodeTtlMinutes = Math.max(1, Math.min(60, resetCodeTtlMinutes));
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthMeResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        validatePassword(request.password());
        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? username
                : request.displayName().trim();

        if (authMapper.findUserByUsername(username) != null) {
            throw new ConflictException("用户名已被使用");
        }
        if (authMapper.findUserByEmail(email) != null) {
            throw new ConflictException("邮箱已被使用");
        }

        UserAccountRow user = new UserAccountRow();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER.name());
        user.setEnabled(true);
        user.setDailyGoal(20);
        if (authMapper.insertUser(user) != 1) {
            throw new ConflictException("注册失败，请重试");
        }

        LocalDateTime now = now();
        authMapper.updateLastLogin(user.getId(), now);
        user.setLastLoginAt(now);
        recordLoginEvent(user.getId(), user.getUsername(), "LOGIN_SUCCESS", httpRequest);
        String csrfToken = establishSession(httpRequest, user);
        return response(user, csrfToken);
    }

    public AuthMeResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String identifier = request.resolvedIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("username or identifier is required");
        }

        UserAccountRow user = authMapper.findUserByIdentifier(identifier);
        boolean passwordMatches = passwordMatches(request.password(), user);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || !passwordMatches) {
            recordLoginEvent(
                    user == null ? null : user.getId(),
                    identifier,
                    "LOGIN_FAILURE",
                    httpRequest
            );
            throw new AuthenticationRequiredException("用户名、邮箱或密码错误");
        }

        LocalDateTime now = now();
        authMapper.updateLastLogin(user.getId(), now);
        user.setLastLoginAt(now);
        recordLoginEvent(user.getId(), user.getUsername(), "LOGIN_SUCCESS", httpRequest);
        String csrfToken = establishSession(httpRequest, user);
        return response(user, csrfToken);
    }

    public AuthMeResponse me(HttpServletRequest request) {
        HttpSession session = requireSession(request);
        long userId = ((Number) session.getAttribute(AuthSession.USER_ID)).longValue();
        UserAccountRow user = authMapper.findUserById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            session.invalidate();
            throw new AuthenticationRequiredException("登录已失效，请重新登录");
        }
        return response(user, String.valueOf(session.getAttribute(AuthSession.CSRF_TOKEN)));
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = requireSession(request);
        long userId = ((Number) session.getAttribute(AuthSession.USER_ID)).longValue();
        UserAccountRow user = authMapper.findUserById(userId);
        recordLoginEvent(
                userId,
                user == null ? String.valueOf(userId) : user.getUsername(),
                "LOGOUT",
                request
        );
        session.invalidate();
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        LocalDateTime now = now();
        LocalDateTime expiresAt = now.plusMinutes(resetCodeTtlMinutes);
        String code = sixDigitCode();
        UserAccountRow user = authMapper.findUserByEmail(normalizeEmail(request.email()));

        if (user != null && Boolean.TRUE.equals(user.getEnabled())) {
            authMapper.invalidateActiveResetTokens(user.getId(), now);
            authMapper.insertResetToken(
                    user.getId(),
                    passwordEncoder.encode(code),
                    expiresAt,
                    now
            );
        } else {
            // Keep the expensive hash operation on the unknown-email path too,
            // reducing the timing difference that could reveal account presence.
            passwordEncoder.encode(code);
        }

        return new ForgotPasswordResponse(
                exposeResetCode && user != null && Boolean.TRUE.equals(user.getEnabled()) ? code : null,
                expiresAt.atOffset(ZoneOffset.UTC)
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        validatePassword(request.newPassword());
        LocalDateTime now = now();
        String email = normalizeEmail(request.email());
        UserAccountRow user = authMapper.findUserByEmail(email);
        List<PasswordResetTokenRow> candidates = user == null
                ? List.of()
                : authMapper.findActiveResetTokens(user.getId(), now);

        PasswordResetTokenRow matched = candidates.stream()
                .filter(token -> passwordEncoder.matches(request.code(), token.getCodeHash()))
                .findFirst()
                .orElse(null);
        if (matched == null) {
            passwordEncoder.matches(request.code(), dummyPasswordHash);
            throw new InvalidResetCodeException("验证码无效或已过期");
        }
        if (authMapper.consumeResetToken(matched.getId(), now) != 1) {
            throw new InvalidResetCodeException("验证码无效或已过期");
        }

        authMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
        authMapper.invalidateActiveResetTokens(user.getId(), now);
        recordLoginEvent(user.getId(), user.getUsername(), "PASSWORD_RESET", httpRequest);
    }

    private boolean passwordMatches(String rawPassword, UserAccountRow user) {
        String encodedPassword = user == null ? dummyPasswordHash : user.getPasswordHash();
        if (encodedPassword == null || encodedPassword.isBlank()) {
            encodedPassword = dummyPasswordHash;
        }
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String establishSession(HttpServletRequest request, UserAccountRow user) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        String csrfToken = csrfToken();
        session.setAttribute(AuthSession.USER_ID, user.getId());
        session.setAttribute(AuthSession.ROLE, user.getRole());
        session.setAttribute(AuthSession.CSRF_TOKEN, csrfToken);
        return csrfToken;
    }

    private HttpSession requireSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(AuthSession.USER_ID) instanceof Number)) {
            throw new AuthenticationRequiredException("请先登录");
        }
        return session;
    }

    private AuthMeResponse response(UserAccountRow user, String csrfToken) {
        return new AuthMeResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.getSelectedBookId(),
                allowShutdown,
                csrfToken
        );
    }

    private void recordLoginEvent(Long userId, String username, String eventType, HttpServletRequest request) {
        try {
            authMapper.insertLoginEvent(
                    userId,
                    truncate(username, 255),
                    eventType,
                    truncate(request.getRemoteAddr(), 64),
                    truncate(request.getHeader("User-Agent"), 512),
                    now()
            );
        } catch (RuntimeException exception) {
            log.warn("Unable to record login event {} for user {}", eventType, username, exception);
        }
    }

    private String sixDigitCode() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    private String csrfToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalArgumentException("password exceeds bcrypt byte limit");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
