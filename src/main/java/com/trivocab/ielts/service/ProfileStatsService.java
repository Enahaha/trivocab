package com.trivocab.ielts.service;

import com.trivocab.ielts.domain.ReviewStatRow;
import com.trivocab.ielts.dto.CheckinResponse;
import com.trivocab.ielts.dto.DailyStudyStat;
import com.trivocab.ielts.dto.StudyStatsResponse;
import com.trivocab.ielts.dto.StudySummary;
import com.trivocab.ielts.mapper.StatsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProfileStatsService {
    private final StatsMapper statsMapper;
    private final Clock clock;
    private final ZoneId applicationZoneId;

    public ProfileStatsService(StatsMapper statsMapper, Clock clock, ZoneId applicationZoneId) {
        this.statsMapper = statsMapper;
        this.clock = clock;
        this.applicationZoneId = applicationZoneId;
    }

    public StudyStatsResponse stats(long userId, String requestedRange) {
        String range = requestedRange == null ? "week" : requestedRange;
        if (!range.equals("week") && !range.equals("month")) {
            throw new IllegalArgumentException("统计范围只支持 week 或 month");
        }
        LocalDate today = LocalDate.now(applicationZoneId);
        LocalDate start = range.equals("month") ? today.withDayOfMonth(1) : today.minusDays(6);

        List<ReviewStatRow> rows = statsMapper.findAllReviewStats(userId);
        Map<LocalDate, long[]> daily = new LinkedHashMap<>();
        long[] totals = new long[3];
        long[] todayValues = new long[3];

        for (ReviewStatRow row : rows) {
            LocalDate date = seoulDate(row.getReviewedAt());
            int learned = row.isFirst() ? 1 : 0;
            int reviewed = row.isFirst() ? 0 : 1;
            totals[0] += learned;
            totals[1] += reviewed;
            totals[2] += row.getResponseMs();
            if (date.equals(today)) {
                todayValues[0] += learned;
                todayValues[1] += reviewed;
                todayValues[2] += row.getResponseMs();
            }
            if (!date.isBefore(start) && !date.isAfter(today)) {
                long[] bucket = daily.computeIfAbsent(date, key -> new long[3]);
                bucket[0] += learned;
                bucket[1] += reviewed;
                bucket[2] += row.getResponseMs();
            }
        }

        List<DailyStudyStat> days = new ArrayList<>();
        for (LocalDate cursor = start; !cursor.isAfter(today); cursor = cursor.plusDays(1)) {
            long[] bucket = daily.getOrDefault(cursor, new long[3]);
            days.add(new DailyStudyStat(
                    cursor,
                    (int) bucket[0],
                    (int) bucket[1],
                    toMinutes(bucket[2])
            ));
        }

        List<LocalDate> checkinDates = statsMapper.findCheckinDates(userId);
        StudySummary summary = new StudySummary(
                (int) totals[0],
                (int) totals[1],
                toMinutes(totals[2]),
                (int) todayValues[0],
                (int) todayValues[1],
                toMinutes(todayValues[2]),
                calculateStreak(checkinDates, today),
                checkinDates.size(),
                checkinDates.contains(today)
        );
        return new StudyStatsResponse(days, summary);
    }

    @Transactional
    public CheckinResponse checkin(long userId) {
        LocalDate today = LocalDate.now(applicationZoneId);
        if (!statsMapper.checkinExists(userId, today)) {
            statsMapper.insertCheckin(userId, today);
        }
        return buildCheckinResponse(userId, today.getYear(), today.getMonthValue());
    }

    public CheckinResponse checkins(long userId, int requestedYear, int requestedMonth) {
        if (requestedYear < 2000 || requestedYear > 2100 || requestedMonth < 1 || requestedMonth > 12) {
            throw new IllegalArgumentException("签到查询的年份或月份不正确");
        }
        return buildCheckinResponse(userId, requestedYear, requestedMonth);
    }

    private CheckinResponse buildCheckinResponse(long userId, int year, int month) {
        LocalDate today = LocalDate.now(applicationZoneId);
        List<LocalDate> all = statsMapper.findCheckinDates(userId);
        List<LocalDate> monthDates = all.stream()
                .filter(date -> date.getYear() == year && date.getMonthValue() == month)
                .toList();
        return new CheckinResponse(
                all.contains(today),
                calculateStreak(all, today),
                all.size(),
                monthDates,
                year,
                month
        );
    }

    private int calculateStreak(List<LocalDate> dates, LocalDate today) {
        Set<LocalDate> active = new HashSet<>(dates);
        LocalDate cursor = active.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (active.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private LocalDate seoulDate(LocalDateTime utcTime) {
        return utcTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(applicationZoneId).toLocalDate();
    }

    private long toMinutes(long responseMs) {
        return Math.round(responseMs / 60000.0);
    }
}
