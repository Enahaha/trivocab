package com.trivocab.ielts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Size(max = 255) String identifier,
        @Size(max = 80) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @Size(max = 64) String timeZone
) {
    public String resolvedIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        return username == null ? null : username.trim();
    }
}
