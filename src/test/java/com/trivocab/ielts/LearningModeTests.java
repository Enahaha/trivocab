package com.trivocab.ielts;

import com.trivocab.ielts.domain.ReviewRating;
import com.trivocab.ielts.domain.UserWordProgressRow;
import com.trivocab.ielts.domain.WordRow;
import com.trivocab.ielts.dto.MeaningOption;
import com.trivocab.ielts.dto.StudyReviewRequest;
import com.trivocab.ielts.dto.WordCardResponse;
import com.trivocab.ielts.mapper.ProgressMapper;
import com.trivocab.ielts.mapper.WordMapper;
import com.trivocab.ielts.service.ProfileService;
import com.trivocab.ielts.service.StudyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-mode-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
class LearningModeTests {
    @Autowired
    private ProfileService profileService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private WordMapper wordMapper;

    @Autowired
    private ProgressMapper progressMapper;

    @Test
    @Transactional
    void defaultLearningModeIsSimple() {
        assertThat(profileService.learningMode(1L)).isEqualTo("SIMPLE");
    }

    @Test
    @Transactional
    void updateLearningModePersistsAndNormalizes() {
        assertThat(profileService.updateLearningMode(1L, "immersive")).isEqualTo("IMMERSIVE");
        assertThat(profileService.learningMode(1L)).isEqualTo("IMMERSIVE");

        assertThat(profileService.updateLearningMode(1L, "simple")).isEqualTo("SIMPLE");
        assertThat(profileService.learningMode(1L)).isEqualTo("SIMPLE");
    }

    @Test
    @Transactional
    void unknownLearningModeIsRejected() {
        assertThatThrownBy(() -> profileService.updateLearningMode(1L, "SOMETHING_ELSE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的学习方式");
    }

    @Test
    @Transactional
    void simpleQueueCarriesNoMultipleChoiceOptions() {
        List<WordCardResponse> queue = studyService.queue(1L, 1L, 5);

        assertThat(queue).isNotEmpty();
        assertThat(queue).allSatisfy(card -> assertThat(card.options()).isNull());
    }

    @Test
    @Transactional
    void immersiveQueueAttachesLookAlikeDistractorsToNewWords() {
        profileService.updateLearningMode(1L, "IMMERSIVE");
        List<WordCardResponse> queue = studyService.queue(1L, 1L, 5);

        List<WordCardResponse> newCards = queue.stream()
                .filter(card -> card.options() != null && !card.options().isEmpty())
                .toList();
        assertThat(newCards).as("IMMERSIVE 队列应包含带干扰项的新词卡片").isNotEmpty();

        WordCardResponse card = newCards.get(0);
        assertThat(card.options()).hasSize(3);
        List<Long> optionIds = card.options().stream().map(MeaningOption::id).toList();
        assertThat(optionIds)
                .doesNotContain(card.id())
                .doesNotHaveDuplicates();

        WordRow word = wordMapper.findById(card.id(), 1L);
        for (MeaningOption option : card.options()) {
            WordRow distractor = wordMapper.findById(option.id(), 1L);
            assertThat(distractor).as("干扰项必须存在").isNotNull();
            assertThat(distractor.getBookId()).as("干扰项必须来自同一词书").isEqualTo(word.getBookId());
        }
    }

    @Test
    @Transactional
    void immersiveQueueLeavesReviewCardsUntouched() {
        // A mastered word that is due returns as a review card without options.
        long wordId = studyService.queue(1L, 1L, 1).getFirst().id();
        studyService.review(1L, new StudyReviewRequest(
                "mode-review-easy", null, wordId, ReviewRating.EASY, 500L
        ));
        UserWordProgressRow progress = progressMapper.findByUserAndWord(1L, wordId);
        progress.setNextReviewAt(java.time.LocalDateTime.now().minusDays(1));
        assertThat(progressMapper.updateProgress(progress)).isEqualTo(1);
        profileService.updateLearningMode(1L, "IMMERSIVE");

        List<WordCardResponse> queue = studyService.queue(1L, 1L, 20);
        List<WordCardResponse> dueCards = queue.stream()
                .filter(card -> "MASTERED".equals(card.progressStatus()))
                .toList();
        assertThat(dueCards).isNotEmpty();
        assertThat(dueCards).allSatisfy(card -> assertThat(card.options()).isNull());
    }
}
