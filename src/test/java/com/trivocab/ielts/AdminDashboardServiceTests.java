package com.trivocab.ielts;

import com.trivocab.ielts.domain.AdminDashboardStatsRow;
import com.trivocab.ielts.dto.AdminDashboardResponse;
import com.trivocab.ielts.mapper.AdminDashboardMapper;
import com.trivocab.ielts.service.AdminDashboardService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTests {

    @Test
    void usesTheSeoulCalendarDayAndMapsAllCounters() {
        AdminDashboardMapper mapper = mock(AdminDashboardMapper.class);
        LocalDateTime startUtc = LocalDateTime.of(2026, 8, 9, 15, 0);
        LocalDateTime endUtc = LocalDateTime.of(2026, 8, 10, 15, 0);
        AdminDashboardStatsRow stats = new AdminDashboardStatsRow();
        stats.setTodayActiveUsers(3L);
        stats.setTodayLoginCount(5L);
        stats.setTodayReviewUsers(2L);
        stats.setTodayReviewCount(41L);
        stats.setTodayNewUsers(1L);
        stats.setTodayMessageCount(4L);
        stats.setTotalUsers(18L);
        stats.setTotalWords(3000L);
        when(mapper.findStats(startUtc, endUtc)).thenReturn(stats);
        AdminDashboardService service = new AdminDashboardService(
                mapper,
                Clock.fixed(Instant.parse("2026-08-09T16:30:00Z"), ZoneOffset.UTC),
                ZoneId.of("Asia/Seoul")
        );

        AdminDashboardResponse response = service.dashboard();

        verify(mapper).findStats(startUtc, endUtc);
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(response.todayActiveUsers()).isEqualTo(3L);
        assertThat(response.todayLoginCount()).isEqualTo(5L);
        assertThat(response.todayReviewCount()).isEqualTo(41L);
        assertThat(response.totalWords()).isEqualTo(3000L);
    }
}
