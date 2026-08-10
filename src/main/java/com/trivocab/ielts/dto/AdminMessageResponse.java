package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.AdminMessageRow;

import java.time.LocalDateTime;

public record AdminMessageResponse(
        long id,
        long userId,
        String username,
        String displayName,
        String email,
        String content,
        String status,
        String adminReply,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminMessageResponse from(AdminMessageRow row) {
        return new AdminMessageResponse(
                row.getId(),
                row.getUserId(),
                row.getUsername(),
                row.getDisplayName(),
                row.getEmail(),
                row.getContent(),
                row.getStatus(),
                row.getAdminReply(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}
