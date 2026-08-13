package com.trivocab.ielts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 80)
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "只能包含英文字母、数字和下划线")
        String username,
        @Size(max = 120) String displayName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @Size(max = 64) String timeZone
) {
}
