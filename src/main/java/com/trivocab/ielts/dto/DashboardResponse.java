package com.trivocab.ielts.dto;

import java.time.LocalDate;

public record DashboardResponse(
        Long bookId,
        String bookName,
        int totalWords,
        int learnedWords,
        int masteredWords,
        int dueWords,
        int todayReviewed,
        int dailyGoal,
        int streakDays,
        double progressPercent,
        int remainingWords,
        int estimatedDays,
        LocalDate estimatedCompletionDate
) {
}
