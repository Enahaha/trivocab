package com.trivocab.ielts.dto;

public record SettingsResponse(
        String learningMode,
        boolean spellingEnabled,
        String meaningDisplay,
        String theme
) {
}
