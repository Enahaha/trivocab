package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.PasswordResetTokenRow;
import com.trivocab.ielts.domain.UserAccountRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuthMapper {
    UserAccountRow findUserById(@Param("userId") long userId);

    UserAccountRow findUserByIdentifier(@Param("identifier") String identifier);

    UserAccountRow findUserByUsername(@Param("username") String username);

    UserAccountRow findUserByEmail(@Param("email") String email);

    int insertUser(UserAccountRow user);

    int updateLastLogin(@Param("userId") long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    int updatePassword(@Param("userId") long userId, @Param("passwordHash") String passwordHash);

    int invalidateActiveResetTokens(
            @Param("userId") long userId,
            @Param("usedAt") LocalDateTime usedAt
    );

    int insertResetToken(
            @Param("userId") long userId,
            @Param("codeHash") String codeHash,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("createdAt") LocalDateTime createdAt
    );

    List<PasswordResetTokenRow> findActiveResetTokens(
            @Param("userId") long userId,
            @Param("now") LocalDateTime now
    );

    int consumeResetToken(@Param("tokenId") long tokenId, @Param("usedAt") LocalDateTime usedAt);

    int insertLoginEvent(
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("eventType") String eventType,
            @Param("ipAddress") String ipAddress,
            @Param("userAgent") String userAgent,
            @Param("createdAt") LocalDateTime createdAt
    );
}
