package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.ReviewRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record StudyReviewRequest(
        @Size(max = 64) String clientReviewId,
        Long sessionId,
        @NotNull Long wordId,
        @NotNull ReviewRating rating,
        @PositiveOrZero Long responseMs
) {
}
