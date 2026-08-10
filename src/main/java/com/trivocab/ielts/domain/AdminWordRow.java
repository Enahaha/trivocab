package com.trivocab.ielts.domain;

import java.time.LocalDateTime;

public class AdminWordRow {
    private Long id;
    private String wordId;
    private Long bookId;
    private String bookName;
    private Integer priorityRank;
    private String word;
    private String phonetic;
    private String partOfSpeech;
    private String chineseMeaning;
    private String koreanMeaning;
    private String koreanEquivalents;
    private String koreanDefinition;
    private String koreanSourceFlag;
    private String englishExample;
    private String koreanExample;
    private String learningStage;
    private String selectionBasis;
    private String sourceName;
    private String sourceUrl;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWordId() { return wordId; }
    public void setWordId(String wordId) { this.wordId = wordId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public Integer getPriorityRank() { return priorityRank; }
    public void setPriorityRank(Integer priorityRank) { this.priorityRank = priorityRank; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }
    public String getPartOfSpeech() { return partOfSpeech; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }
    public String getChineseMeaning() { return chineseMeaning; }
    public void setChineseMeaning(String chineseMeaning) { this.chineseMeaning = chineseMeaning; }
    public String getKoreanMeaning() { return koreanMeaning; }
    public void setKoreanMeaning(String koreanMeaning) { this.koreanMeaning = koreanMeaning; }
    public String getKoreanEquivalents() { return koreanEquivalents; }
    public void setKoreanEquivalents(String koreanEquivalents) { this.koreanEquivalents = koreanEquivalents; }
    public String getKoreanDefinition() { return koreanDefinition; }
    public void setKoreanDefinition(String koreanDefinition) { this.koreanDefinition = koreanDefinition; }
    public String getKoreanSourceFlag() { return koreanSourceFlag; }
    public void setKoreanSourceFlag(String koreanSourceFlag) { this.koreanSourceFlag = koreanSourceFlag; }
    public String getEnglishExample() { return englishExample; }
    public void setEnglishExample(String englishExample) { this.englishExample = englishExample; }
    public String getKoreanExample() { return koreanExample; }
    public void setKoreanExample(String koreanExample) { this.koreanExample = koreanExample; }
    public String getLearningStage() { return learningStage; }
    public void setLearningStage(String learningStage) { this.learningStage = learningStage; }
    public String getSelectionBasis() { return selectionBasis; }
    public void setSelectionBasis(String selectionBasis) { this.selectionBasis = selectionBasis; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
