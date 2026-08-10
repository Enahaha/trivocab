package com.trivocab.ielts.dto;

import java.time.OffsetDateTime;

public record ForgotPasswordResponse(
        String resetCode,
        OffsetDateTime expiresAt
) {
}
