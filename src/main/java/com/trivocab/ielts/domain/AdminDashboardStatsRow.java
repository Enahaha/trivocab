package com.trivocab.ielts.domain;

public class AdminDashboardStatsRow {
    private Long todayActiveUsers;
    private Long todayLoginCount;
    private Long todayReviewUsers;
    private Long todayReviewCount;
    private Long todayNewUsers;
    private Long todayMessageCount;
    private Long totalUsers;
    private Long totalWords;

    public Long getTodayActiveUsers() { return todayActiveUsers; }
    public void setTodayActiveUsers(Long todayActiveUsers) { this.todayActiveUsers = todayActiveUsers; }
    public Long getTodayLoginCount() { return todayLoginCount; }
    public void setTodayLoginCount(Long todayLoginCount) { this.todayLoginCount = todayLoginCount; }
    public Long getTodayReviewUsers() { return todayReviewUsers; }
    public void setTodayReviewUsers(Long todayReviewUsers) { this.todayReviewUsers = todayReviewUsers; }
    public Long getTodayReviewCount() { return todayReviewCount; }
    public void setTodayReviewCount(Long todayReviewCount) { this.todayReviewCount = todayReviewCount; }
    public Long getTodayNewUsers() { return todayNewUsers; }
    public void setTodayNewUsers(Long todayNewUsers) { this.todayNewUsers = todayNewUsers; }
    public Long getTodayMessageCount() { return todayMessageCount; }
    public void setTodayMessageCount(Long todayMessageCount) { this.todayMessageCount = todayMessageCount; }
    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
    public Long getTotalWords() { return totalWords; }
    public void setTotalWords(Long totalWords) { this.totalWords = totalWords; }
}
