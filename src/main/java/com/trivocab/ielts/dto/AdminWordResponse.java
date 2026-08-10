package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.AdminWordRow;

import java.time.LocalDateTime;

public record AdminWordResponse(
        long id,
        String wordId,
        long bookId,
        String bookName,
        int priorityRank,
        String word,
        String phonetic,
        String partOfSpeech,
        String chineseMeaning,
        String koreanMeaning,
        String koreanEquivalents,
        String koreanDefinition,
        String koreanSourceFlag,
        String englishExample,
        String koreanExample,
        String learningStage,
        String selectionBasis,
        String sourceName,
        String sourceUrl,
        LocalDateTime createdAt
) {
    public static AdminWordResponse from(AdminWordRow row) {
        return new AdminWordResponse(
                row.getId(),
                row.getWordId(),
                row.getBookId(),
                row.getBookName(),
                row.getPriorityRank(),
                row.getWord(),
                row.getPhonetic(),
                row.getPartOfSpeech(),
                row.getChineseMeaning(),
                row.getKoreanMeaning(),
                row.getKoreanEquivalents(),
                row.getKoreanDefinition(),
                row.getKoreanSourceFlag(),
                row.getEnglishExample(),
                row.getKoreanExample(),
                row.getLearningStage(),
                row.getSelectionBasis(),
                row.getSourceName(),
                row.getSourceUrl(),
                row.getCreatedAt()
        );
    }
}
