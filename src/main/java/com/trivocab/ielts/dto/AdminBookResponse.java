package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.AdminBookRow;

import java.time.LocalDateTime;

public record AdminBookResponse(
        long id,
        String code,
        String name,
        String description,
        int totalWords,
        LocalDateTime createdAt
) {
    public static AdminBookResponse from(AdminBookRow row) {
        return new AdminBookResponse(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getDescription(),
                row.getTotalWords() == null ? 0 : row.getTotalWords(),
                row.getCreatedAt()
        );
    }
}
