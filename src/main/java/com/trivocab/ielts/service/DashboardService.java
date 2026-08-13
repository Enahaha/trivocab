package com.trivocab.ielts.service;

import com.trivocab.ielts.common.UserTimezoneProvider;
import com.trivocab.ielts.domain.DashboardStatsRow;
import com.trivocab.ielts.dto.BookSummaryResponse;
import com.trivocab.ielts.dto.DashboardResponse;
import com.trivocab.ielts.mapper.DashboardMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DashboardService {
    private final DashboardMapper dashboardMapper;
    private final VocabularyService vocabularyService;
    private final Clock clock;
    private final UserTimezoneProvider userTimezoneProvider;

    public DashboardService(
            DashboardMapper dashboardMapper,
            VocabularyService vocabularyService,
            Clock clock,
            UserTimezoneProvider userTimezoneProvider
    ) {
        this.dashboardMapper = dashboardMapper;
        this.vocabularyService = vocabularyService;
        this.clock = clock;
        this.userTimezoneProvider = userTimezoneProvider;
    }

    public DashboardResponse dashboard(long bookId, long userId) {
        BookSummaryResponse book = vocabularyService.getBook(bookId, userId);
        Instant instant = clock.instant();
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        ZoneId userZone = userTimezoneProvider.zoneOf(userId);
        ZonedDateTime localNow = instant.atZone(userZone);
        LocalDate today = localNow.toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay(userZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay(userZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        DashboardStatsRow stats = dashboardMapper.findStats(
                bookId, userId, now, dayStart, dayEnd
        );
        int streakDays = calculateStreak(
                dashboardMapper.findReviewTimesBefore(userId, dayEnd),
                today,
                userZone
        );

        int total = value(stats.getTotalWords());
        int learned = value(stats.getLearnedWords());
        int dailyGoal = value(stats.getDailyGoal());
        int remaining = Math.max(0, total - learned);
        int estimatedDays = estimateDays(remaining, dailyGoal);
        LocalDate estimatedCompletionDate = estimatedDays == 0
                ? today
                : today.plusDays(estimatedDays - 1L);
        double percent = total == 0 ? 0.0 : Math.round(learned * 1000.0 / total) / 10.0;
        return new DashboardResponse(
                bookId,
                book.name(),
                total,
                learned,
                value(stats.getMasteredWords()),
                value(stats.getDueWords()),
                value(stats.getTodayReviewed()),
                dailyGoal,
                streakDays,
                percent,
                remaining,
                estimatedDays,
                estimatedCompletionDate
        );
    }

    private int calculateStreak(List<LocalDateTime> reviewTimes, LocalDate today, ZoneId userZone) {
        Set<LocalDate> activeDays = new HashSet<>();
        for (LocalDateTime reviewTime : reviewTimes) {
            LocalDate localDate = reviewTime
                    .atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(userZone)
                    .toLocalDate();
            activeDays.add(localDate);
        }

        LocalDate cursor = activeDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (activeDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private int estimateDays(int remainingWords, int dailyGoal) {
        if (remainingWords == 0) {
            return 0;
        }
        if (dailyGoal <= 0) {
            // Legacy databases may contain an invalid value from an older
            // version. Avoid division by zero until the user saves a new plan.
            return 0;
        }
        return (remainingWords + dailyGoal - 1) / dailyGoal;
    }
}
