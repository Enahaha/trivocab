package com.trivocab.ielts.dto;

import java.time.LocalDate;

public record BookSelectionItem(
        Long id,
        String code,
        String name,
        String description,
        int totalWords,
        int learnedWords,
        double progressPercent,
        int dailyGoal,
        int remainingWords,
        int estimatedDays,
        LocalDate estimatedCompletionDate,
        boolean selected
) {
}
