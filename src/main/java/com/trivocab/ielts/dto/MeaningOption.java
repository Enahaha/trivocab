package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.WordRow;

/**
 * One multiple-choice meaning option for the immersive first-encounter card.
 * Distractors are look-alike words from the same book, so the correct answer
 * cannot be guessed from a foreign spelling.
 */
public record MeaningOption(
        Long id,
        String word,
        String chineseMeaning,
        String koreanMeaning
) {
    public static MeaningOption from(WordRow row) {
        return new MeaningOption(
                row.getId(),
                row.getWord(),
                row.getChineseMeaning(),
                row.getKoreanMeaning()
        );
    }
}
