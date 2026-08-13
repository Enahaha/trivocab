package com.trivocab.ielts.domain;

import java.time.LocalDateTime;

public class UserWordProgressRow {
    private Long id;
    private Long userId;
    private Long wordId;
    private String status;
    private Double easeFactor;
    private Integer intervalDays;
    /**
     * Interval in days produced by the last successful (GOOD/EASY) review.
     * Kept stable across AGAIN/HARD so the SM-2 style curve can continue
     * from the last learned interval instead of resetting to zero.
     */
    private Integer lastIntervalDays;
    private Integer repetitions;
    private LocalDateTime nextReviewAt;
    private LocalDateTime lastReviewedAt;
    private Integer correctCount;
    private Integer wrongCount;
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getWordId() { return wordId; }
    public void setWordId(Long wordId) { this.wordId = wordId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(Double easeFactor) { this.easeFactor = easeFactor; }
    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
    public Integer getLastIntervalDays() { return lastIntervalDays; }
    public void setLastIntervalDays(Integer lastIntervalDays) { this.lastIntervalDays = lastIntervalDays; }
    public Integer getRepetitions() { return repetitions; }
    public void setRepetitions(Integer repetitions) { this.repetitions = repetitions; }
    public LocalDateTime getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(LocalDateTime nextReviewAt) { this.nextReviewAt = nextReviewAt; }
    public LocalDateTime getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(LocalDateTime lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
    public Integer getCorrectCount() { return correctCount; }
    public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }
    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
