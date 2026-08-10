package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.WordBookRow;

public record BookSummaryResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer totalWords,
        Integer learnedWords,
        double progressPercent
) {
    public static BookSummaryResponse from(WordBookRow row) {
        int total = row.getTotalWords() == null ? 0 : row.getTotalWords();
        int learned = row.getLearnedWords() == null ? 0 : row.getLearnedWords();
        double percent = total == 0 ? 0.0 : Math.round(learned * 1000.0 / total) / 10.0;
        return new BookSummaryResponse(
                row.getId(), row.getCode(), row.getName(), row.getDescription(), total, learned, percent
        );
    }
}
