package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.ReviewRating;

import java.time.OffsetDateTime;

public record StudyReviewResponse(
        Long wordId,
        /**
         * Kept for compatibility with the first frontend contract. New clients
         * should use {@link #progressStatus()}.
         */
        String status,
        String progressStatus,
        ReviewRating rating,
        boolean repeatInSession,
        int repeatAfterCards,
        int repetitions,
        int intervalDays,
        OffsetDateTime nextReviewAt
) {
}
