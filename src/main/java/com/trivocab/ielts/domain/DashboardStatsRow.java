package com.trivocab.ielts.domain;

public class DashboardStatsRow {
    private Integer totalWords;
    private Integer learnedWords;
    private Integer masteredWords;
    private Integer dueWords;
    private Integer todayReviewed;
    private Integer dailyGoal;

    public Integer getTotalWords() { return totalWords; }
    public void setTotalWords(Integer totalWords) { this.totalWords = totalWords; }
    public Integer getLearnedWords() { return learnedWords; }
    public void setLearnedWords(Integer learnedWords) { this.learnedWords = learnedWords; }
    public Integer getMasteredWords() { return masteredWords; }
    public void setMasteredWords(Integer masteredWords) { this.masteredWords = masteredWords; }
    public Integer getDueWords() { return dueWords; }
    public void setDueWords(Integer dueWords) { this.dueWords = dueWords; }
    public Integer getTodayReviewed() { return todayReviewed; }
    public void setTodayReviewed(Integer todayReviewed) { this.todayReviewed = todayReviewed; }
    public Integer getDailyGoal() { return dailyGoal; }
    public void setDailyGoal(Integer dailyGoal) { this.dailyGoal = dailyGoal; }
}
