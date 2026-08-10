package com.trivocab.ielts.service;

import com.trivocab.ielts.domain.AdminDashboardStatsRow;
import com.trivocab.ielts.dto.AdminDashboardResponse;
import com.trivocab.ielts.mapper.AdminDashboardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Service
public class AdminDashboardService {
    private final AdminDashboardMapper adminDashboardMapper;
    private final Clock clock;
    private final ZoneId applicationZoneId;

    public AdminDashboardService(
            AdminDashboardMapper adminDashboardMapper,
            Clock clock,
            ZoneId applicationZoneId
    ) {
        this.adminDashboardMapper = adminDashboardMapper;
        this.clock = clock;
        this.applicationZoneId = applicationZoneId;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        Instant instant = clock.instant();
        LocalDate today = instant.atZone(applicationZoneId).toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay(applicationZoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay(applicationZoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        AdminDashboardStatsRow stats = adminDashboardMapper.findStats(dayStart, dayEnd);
        if (stats == null) {
            stats = new AdminDashboardStatsRow();
        }
        return AdminDashboardResponse.from(today, stats);
    }
}
