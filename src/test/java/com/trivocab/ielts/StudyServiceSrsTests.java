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
    void goodEndsTheInSessionLoopAndFollowsTheSm2CurveDrivenByEase() {
        long wordId = firstNewWord().id();
        // ease stays 2.5 across GOOD reviews: 1, round(1*2.5)=3, round(3*2.5)=8,
        // round(8*2.5)=20, round(20*2.5)=50, round(50*2.5)=125, round(125*2.5)=313
        int[] expectedIntervals = {1, 3, 8, 20, 50, 125, 313};

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
    void easyMastersImmediatelyButComesBackAfterTheMasteryInterval() {
        long wordId = firstNewWord().id();

        StudyReviewResponse response = review(wordId, ReviewRating.EASY, "srs-easy");

        assertThat(response.progressStatus()).isEqualTo(ProgressStatus.MASTERED.name());
        assertThat(response.repeatInSession()).isFalse();
        assertThat(response.repeatAfterCards()).isZero();
        assertThat(response.intervalDays()).isEqualTo(30);
        assertThat(response.nextReviewAt().toInstant()).isEqualTo(NOW.plus(Duration.ofDays(30)));

        // Not due yet: mastered words do not re-enter the queue before the interval.
        List<WordCardResponse> queue = studyService.queue(1L, 1L, 100);
        assertThat(queue).extracting(WordCardResponse::id).doesNotContain(wordId);
        assertThat(dashboardService.dashboard(1L, 1L).dueWords()).isZero();

        // Once the mastery interval passes, the word comes back for review.
        UserWordProgressRow mastered = progressMapper.findByUserAndWord(1L, wordId);
        mastered.setNextReviewAt(LocalDateTime.ofInstant(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC));
        assertThat(progressMapper.updateProgress(mastered)).isEqualTo(1);

        assertThat(studyService.queue(1L, 1L, 100))
                .extracting(WordCardResponse::id).contains(wordId);
        assertThat(dashboardService.dashboard(1L, 1L).dueWords()).isEqualTo(1);
    }

    @Test
    @Transactional
    void masteredWordsExtendTheIntervalWhenRememberedAndDowngradeWhenForgotten() {
        long wordId = firstNewWord().id();
        review(wordId, ReviewRating.EASY, "srs-mastered-easy");
        markDue(wordId);

        // Remembered: stays mastered, interval grows to 30 * 2.65 = 80 days.
        StudyReviewResponse good = review(wordId, ReviewRating.GOOD, "srs-mastered-good");
        assertThat(good.progressStatus()).isEqualTo(ProgressStatus.MASTERED.name());
        assertThat(good.intervalDays()).isEqualTo(80);
        assertThat(good.nextReviewAt().toInstant()).isEqualTo(NOW.plus(Duration.ofDays(80)));

        // Forgotten: AGAIN downgrades back to NEW with a 10-minute revisit.
        markDue(wordId);
        StudyReviewResponse again = review(wordId, ReviewRating.AGAIN, "srs-mastered-again");
        assertThat(again.progressStatus()).isEqualTo(ProgressStatus.NEW.name());
        assertThat(again.intervalDays()).isZero();
        assertThat(again.nextReviewAt().toInstant()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    @Transactional
    void hardNeverInflatesTheCurveAndTheNextGoodRestartsAtOneDay() {
        long wordId = firstNewWord().id();
        review(wordId, ReviewRating.GOOD, "srs-hard-good-1");
        review(wordId, ReviewRating.GOOD, "srs-hard-good-2");
        assertThat(progressMapper.findByUserAndWord(1L, wordId).getIntervalDays()).isEqualTo(3);

        review(wordId, ReviewRating.HARD, "srs-hard-hard");
        StudyReviewResponse learnedAgain = review(wordId, ReviewRating.GOOD, "srs-hard-good-3");

        // A fuzzy answer must not turn into a longer schedule on the next GOOD.
        assertThat(learnedAgain.intervalDays()).isEqualTo(1);
        assertThat(learnedAgain.nextReviewAt().toInstant()).isEqualTo(NOW.plus(Duration.ofDays(1)));
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

    private void markDue(long wordId) {
        UserWordProgressRow progress = progressMapper.findByUserAndWord(1L, wordId);
        progress.setNextReviewAt(LocalDateTime.ofInstant(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC));
        assertThat(progressMapper.updateProgress(progress)).isEqualTo(1);
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
