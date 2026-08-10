package com.trivocab.ielts.dto;

public record StudySummary(
        int totalLearnedWords,
        int totalReviewedWords,
        long totalStudyMinutes,
        int todayLearnedWords,
        int todayReviewedWords,
        long todayStudyMinutes,
        int checkinStreak,
        int totalCheckins,
        boolean todayCheckedIn
) {
}
