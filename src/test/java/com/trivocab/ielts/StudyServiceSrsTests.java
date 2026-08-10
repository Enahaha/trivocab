package com.trivocab.ielts;

import com.trivocab.ielts.domain.ProgressStatus;
import com.trivocab.ielts.domain.ReviewRating;
import com.trivocab.ielts.domain.UserWordProgressRow;
import com.trivocab.ielts.dto.DashboardResponse;
import com.trivocab.ielts.dto.StudyReviewRequest;
import com.trivocab.ielts.dto.StudyReviewResponse;
import com.trivocab.ielts.dto.WordCardResponse;
import com.trivocab.ielts.mapper.ProgressMapper;
import com.trivocab.ielts.service.DashboardService;
import com.trivocab.ielts.service.StudyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-srs-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
@Import(StudyServiceSrsTests.FixedClockConfiguration.class)
class StudyServiceSrsTests {
    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");

    @Autowired
    private StudyService studyService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ProgressMapper progressMapper;

    @Test
    @Transactional
    void newWordsDefaultToTheUnfamiliarStage() {
        WordCardResponse word = firstNewWord();

        assertThat(word.progressStatus()).isEqualTo(ProgressStatus.NEW.name());
    }

    @Test
    @Transactional
    void againAndHardRequestAnotherInSessionPassAndKeepANearTermDueTime() {
        long wordId = firstNewWord().id();

        StudyReviewResponse again = review(wordId, ReviewRating.AGAIN, "srs-again");
        StudyReviewResponse hard = review(wordId, ReviewRating.HARD, "srs-hard");

        assertThat(again.progressStatus()).isEqualTo(ProgressStatus.NEW.name());
        assertThat(again.status()).isEqualTo(again.progressStatus());
        assertThat(again.repeatInSession()).isTrue();
        assertThat(again.repeatAfterCards()).isEqualTo(3);
        assertThat(again.repetitions()).isZero();
        assertThat(again.intervalDays()).isZero();
        assertThat(again.nextReviewAt().toInstant()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));

        assertThat(hard.progressStatus()).isEqualTo(ProgressStatus.LEARNING.name());
        assertThat(hard.repeatInSession()).isTrue();
        assertThat(hard.repeatAfterCards()).isEqualTo(3);
        assertThat(hard.repetitions()).isZero();
        assertThat(hard.intervalDays()).isZero();
        assertThat(hard.nextReviewAt().toInstant()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));

        UserWordProgressRow persisted = progressMapper.findByUserAndWord(1L, wordId);
        assertThat(persisted.getWrongCount()).isEqualTo(2);
    }

    @Test
    @Transactional
    void goodEndsTheInSessionLoopAndFollowsTheForgettingCurve() {
        long wordId = firstNewWord().id();
        int[] expectedIntervals = {1, 2, 4, 7, 15, 30, 60};

        for (int index = 0; index < expectedIntervals.length; index++) {
            StudyReviewResponse response = review(
                    wordId,
                    ReviewRating.GOOD,
                    "srs-good-" + index
            );

            assertThat(response.progressStatus()).isEqualTo(ProgressStatus.REVIEWING.name());
            assertThat(response.repeatInSession()).isFalse();
            assertThat(response.repeatAfterCards()).isZero();
            assertThat(response.repetitions()).isEqualTo(index + 1);
            assertThat(response.intervalDays()).isEqualTo(expectedIntervals[index]);
            assertThat(response.nextReviewAt().toInstant())
                    .isEqualTo(NOW.plus(Duration.ofDays(expectedIntervals[index])));
        }
    }

    @Test
    @Transactional
    void againResetsTheLongTermCurveBeforeTheNextGoodAnswer() {
        long wordId = firstNewWord().id();
        review(wordId, ReviewRating.GOOD, "srs-reset-good-1");
        review(wordId, ReviewRating.GOOD, "srs-reset-good-2");

        StudyReviewResponse again = review(wordId, ReviewRating.AGAIN, "srs-reset-again");
        StudyReviewResponse learnedAgain = review(wordId, ReviewRating.GOOD, "srs-reset-good-3");

        assertThat(again.repetitions()).isZero();
        assertThat(learnedAgain.repetitions()).isEqualTo(1);
        assertThat(learnedAgain.intervalDays()).isEqualTo(1);
    }

    @Test
    @Transactional
    void easyMastersImmediatelyAndMasteredWordsNeverEnterTheDueQueue() {
        long wordId = firstNewWord().id();

        StudyReviewResponse response = review(wordId, ReviewRating.EASY, "srs-easy");

        assertThat(response.progressStatus()).isEqualTo(ProgressStatus.MASTERED.name());
        assertThat(response.repeatInSession()).isFalse();
        assertThat(response.repeatAfterCards()).isZero();
        assertThat(response.intervalDays()).isZero();
        assertThat(response.nextReviewAt()).isNull();

        // Even corrupt/legacy timestamps must not make a mastered row due.
        UserWordProgressRow mastered = progressMapper.findByUserAndWord(1L, wordId);
        mastered.setNextReviewAt(LocalDateTime.ofInstant(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC));
        assertThat(progressMapper.updateProgress(mastered)).isEqualTo(1);

        List<WordCardResponse> nextQueue = studyService.queue(1L, 1L, 100);
        DashboardResponse dashboard = dashboardService.dashboard(1L, 1L);
        assertThat(nextQueue).extracting(WordCardResponse::id).doesNotContain(wordId);
        assertThat(dashboard.masteredWords()).isEqualTo(1);
        assertThat(dashboard.dueWords()).isZero();
    }

    @Test
    @Transactional
    void repeatedAttemptsOfOneWordCountOnceTowardTheDailyGoal() {
        List<WordCardResponse> words = studyService.queue(1L, 1L, 2);
        long firstWordId = words.get(0).id();
        long secondWordId = words.get(1).id();

        review(firstWordId, ReviewRating.AGAIN, "srs-daily-again");
        review(firstWordId, ReviewRating.HARD, "srs-daily-hard");
        review(firstWordId, ReviewRating.GOOD, "srs-daily-good");

        assertThat(dashboardService.dashboard(1L, 1L).todayReviewed()).isEqualTo(1);

        review(secondWordId, ReviewRating.GOOD, "srs-daily-second-word");

        assertThat(dashboardService.dashboard(1L, 1L).todayReviewed()).isEqualTo(2);
    }

    private WordCardResponse firstNewWord() {
        return studyService.queue(1L, 1L, 1).getFirst();
    }

    private StudyReviewResponse review(long wordId, ReviewRating rating, String clientReviewId) {
        return studyService.review(
                1L,
                new StudyReviewRequest(clientReviewId, null, wordId, rating, 800L)
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedSrsClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
