package com.trivocab.ielts.dto;

import java.time.LocalDate;

public record DailyGoalResponse(
        Integer dailyGoal,
        Long bookId,
        int remainingWords,
        int estimatedDays,
        LocalDate estimatedCompletionDate
) {
}
