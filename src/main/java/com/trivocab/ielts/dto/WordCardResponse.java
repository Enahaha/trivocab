package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.WordRow;

import java.util.List;

public record WordCardResponse(
        Long id,
        Integer priorityRank,
        String word,
        String phonetic,
        String partOfSpeech,
        String chineseMeaning,
        String koreanMeaning,
        String koreanEquivalents,
        String koreanDefinition,
        String englishExample,
        String koreanExample,
        String learningStage,
        String selectionBasis,
        String progressStatus,
        /**
         * Multiple-choice meaning options for the immersive first-encounter
         * card. {@code null} on regular review cards and in SIMPLE mode.
         */
        List<MeaningOption> options
) {
    public static WordCardResponse from(WordRow row) {
        return from(row, null);
    }

    public static WordCardResponse from(WordRow row, List<MeaningOption> options) {
        return new WordCardResponse(
                row.getId(),
                row.getPriorityRank(),
                row.getWord(),
                row.getPhonetic(),
                row.getPartOfSpeech(),
                row.getChineseMeaning(),
                row.getKoreanMeaning(),
                row.getKoreanEquivalents(),
                row.getKoreanDefinition(),
                row.getEnglishExample(),
                row.getKoreanExample(),
                row.getLearningStage(),
                row.getSelectionBasis(),
                row.getProgressStatus() == null ? "NEW" : row.getProgressStatus(),
                options
        );
    }
}
