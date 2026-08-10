package com.trivocab.ielts.domain;

import java.time.LocalDateTime;

public class ReviewStatRow {
    private LocalDateTime reviewedAt;
    private long responseMs;
    private boolean first;

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public long getResponseMs() { return responseMs; }
    public void setResponseMs(long responseMs) { this.responseMs = responseMs; }
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
}
