package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.MessageRow;

import java.time.LocalDateTime;

public record MessageResponse(
        long id,
        long userId,
        String content,
        String status,
        String adminReply,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MessageResponse from(MessageRow row) {
        return new MessageResponse(
                row.getId(),
                row.getUserId(),
                row.getContent(),
                row.getStatus(),
                row.getAdminReply(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}
