package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.AdminUserRow;

import java.time.LocalDateTime;

public record AdminUserResponse(
        long id,
        String username,
        String displayName,
        String email,
        String role,
        boolean enabled,
        int dailyGoal,
        int learnedWords,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminUserResponse from(AdminUserRow row) {
        return new AdminUserResponse(
                row.getId(),
                row.getUsername(),
                row.getDisplayName(),
                row.getEmail(),
                row.getRole(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getDailyGoal() == null ? 0 : row.getDailyGoal(),
                row.getLearnedWords() == null ? 0 : row.getLearnedWords(),
                row.getLastLoginAt(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}
