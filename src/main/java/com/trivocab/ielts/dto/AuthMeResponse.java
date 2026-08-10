package com.trivocab.ielts.dto;

public record AuthMeResponse(
        Long id,
        String username,
        String displayName,
        String email,
        String role,
        Long selectedBookId,
        String csrfToken
) {
}
