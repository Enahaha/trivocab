package com.trivocab.ielts.dto;

import com.trivocab.ielts.domain.AdminDashboardStatsRow;

import java.time.LocalDate;

public record AdminDashboardResponse(
        LocalDate date,
        long todayActiveUsers,
        long todayLoginCount,
        long todayReviewUsers,
        long todayReviewCount,
        long todayNewUsers,
        long todayMessageCount,
        long totalUsers,
        long totalWords
) {
    public static AdminDashboardResponse from(LocalDate date, AdminDashboardStatsRow row) {
        return new AdminDashboardResponse(
                date,
                value(row.getTodayActiveUsers()),
                value(row.getTodayLoginCount()),
                value(row.getTodayReviewUsers()),
                value(row.getTodayReviewCount()),
                value(row.getTodayNewUsers()),
                value(row.getTodayMessageCount()),
                value(row.getTotalUsers()),
                value(row.getTotalWords())
        );
    }

    private static long value(Long number) {
        return number == null ? 0L : number;
    }
}
