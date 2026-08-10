package com.trivocab.ielts;

import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.domain.ReviewRating;
import com.trivocab.ielts.dto.DashboardResponse;
import com.trivocab.ielts.dto.StudyReviewRequest;
import com.trivocab.ielts.dto.StudyReviewResponse;
import com.trivocab.ielts.dto.WordCardResponse;
import com.trivocab.ielts.mapper.ProgressMapper;
import com.trivocab.ielts.service.DashboardService;
import com.trivocab.ielts.service.StudyService;
import com.trivocab.ielts.service.VocabularyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
@Import(IeltsVocabularyApplicationTests.FixedClockConfiguration.class)
class IeltsVocabularyApplicationTests {

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ProgressMapper progressMapper;

    @Autowired
    private Clock clock;

    @Autowired
    private ZoneId applicationZoneId;

    @Test
    void loadsTheCompleteTrilingualIeltsWordBook() {
        PageResult<WordCardResponse> page = vocabularyService.listWords(1L, 1L, 0, 20, null);

        assertThat(page.total()).isEqualTo(3000);
        assertThat(page.items()).hasSize(20);
        assertThat(page.items().getFirst().chineseMeaning()).isNotBlank();
        assertThat(page.items().getFirst().koreanMeaning()).isNotBlank();
    }

    @Test
    void searchesEnglishChineseAndKoreanMeanings() {
        PageResult<WordCardResponse> english = vocabularyService.listWords(1L, 1L, 0, 20, "environment");
        PageResult<WordCardResponse> chinese = vocabularyService.listWords(1L, 1L, 0, 20, "环境");
        PageResult<WordCardResponse> korean = vocabularyService.listWords(1L, 1L, 0, 20, "환경");

        assertThat(english.total()).isPositive();
        assertThat(chinese.total()).isPositive();
        assertThat(korean.total()).isPositive();
    }

    @Test
    @Transactional
    void recordsAReviewAndUpdatesTheDashboard() {
        List<WordCardResponse> queue = studyService.queue(1L, 1L, 5);
        WordCardResponse word = queue.getFirst();
        StudyReviewRequest request = new StudyReviewRequest(
                "integration-test-review",
                null,
                word.id(),
                ReviewRating.GOOD,
                1200L
        );

        StudyReviewResponse reviewed = studyService.review(1L, request);
        StudyReviewResponse duplicate = studyService.review(1L, request);
        DashboardResponse firstDayDashboard = dashboardService.dashboard(1L, 1L);
        assertThat(firstDayDashboard.streakDays()).isEqualTo(1);

        LocalDate today = clock.instant().atZone(applicationZoneId).toLocalDate();
        progressMapper.insertReviewLog(
                "integration-test-yesterday",
                1L,
                word.id(),
                null,
                ReviewRating.GOOD.name(),
                900L,
                toUtc(today.minusDays(1))
        );
        progressMapper.insertReviewLog(
                "integration-test-two-days-ago",
                1L,
                word.id(),
                null,
                ReviewRating.GOOD.name(),
                800L,
                toUtc(today.minusDays(2))
        );
        DashboardResponse dashboard = dashboardService.dashboard(1L, 1L);

        assertThat(reviewed.wordId()).isEqualTo(word.id());
        assertThat(reviewed.repetitions()).isEqualTo(1);
        assertThat(reviewed.intervalDays()).isEqualTo(1);
        assertThat(duplicate.repetitions()).isEqualTo(1);
        assertThat(dashboard.learnedWords()).isEqualTo(1);
        assertThat(dashboard.todayReviewed()).isEqualTo(1);
        assertThat(dashboard.streakDays()).isEqualTo(3);
    }

    @Test
    @Transactional
    void keepsTheYesterdayStreakButStopsAtAGap() {
        LocalDate today = clock.instant().atZone(applicationZoneId).toLocalDate();
        insertReview("gap-two-days-ago", toUtc(today.minusDays(2)));

        assertThat(dashboardService.dashboard(1L, 1L).streakDays()).isZero();

        insertReview("gap-yesterday", toUtc(today.minusDays(1)));

        assertThat(dashboardService.dashboard(1L, 1L).streakDays()).isEqualTo(2);
    }

    @Test
    @Transactional
    void usesSeoulMidnightAsTheStudyDayBoundary() {
        LocalDate today = clock.instant().atZone(applicationZoneId).toLocalDate();
        LocalDateTime todayStartUtc = today.atStartOfDay(applicationZoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        insertReview("seoul-before-midnight", todayStartUtc.minusMinutes(1));
        insertReview("seoul-at-midnight", todayStartUtc);

        DashboardResponse dashboard = dashboardService.dashboard(1L, 1L);

        assertThat(dashboard.todayReviewed()).isEqualTo(1);
        assertThat(dashboard.streakDays()).isEqualTo(2);
    }

    @Test
    @Transactional
    void multipleAttemptsOfTheSameWordCountOnceAndStillProduceOneStreakDay() {
        LocalDate today = clock.instant().atZone(applicationZoneId).toLocalDate();
        insertReview("same-day-one", toUtc(today));
        insertReview("same-day-two", toUtc(today).plusHours(1));

        DashboardResponse dashboard = dashboardService.dashboard(1L, 1L);

        assertThat(dashboard.todayReviewed()).isEqualTo(1);
        assertThat(dashboard.streakDays()).isEqualTo(1);
    }

    private void insertReview(String clientReviewId, LocalDateTime reviewedAt) {
        progressMapper.insertReviewLog(
                clientReviewId,
                1L,
                1L,
                null,
                ReviewRating.GOOD.name(),
                500L,
                reviewedAt
        );
    }

    private LocalDateTime toUtc(LocalDate localDate) {
        return localDate.atTime(LocalTime.NOON)
                .atZone(applicationZoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedTestClock() {
            return Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
