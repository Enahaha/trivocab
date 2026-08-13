package com.trivocab.ielts.service;

import com.trivocab.ielts.domain.ProgressStatus;
import com.trivocab.ielts.domain.ReviewRating;
import com.trivocab.ielts.domain.UserWordProgressRow;
import com.trivocab.ielts.domain.WordRow;
import com.trivocab.ielts.dto.MeaningOption;
import com.trivocab.ielts.dto.StudyReviewRequest;
import com.trivocab.ielts.dto.StudyReviewResponse;
import com.trivocab.ielts.dto.WordCardResponse;
import com.trivocab.ielts.exception.ConflictException;
import com.trivocab.ielts.mapper.ProgressMapper;
import com.trivocab.ielts.mapper.UserBookMapper;
import com.trivocab.ielts.mapper.WordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StudyService {
    private static final double MIN_EASE = 1.3;
    private static final double MAX_EASE = 3.0;
    private static final int LOW_RATING_REPEAT_AFTER_CARDS = 3;
    private static final int FIRST_GOOD_INTERVAL_DAYS = 1;
    private static final int EASY_INITIAL_INTERVAL_DAYS = 30;
    private static final int MAX_INTERVAL_DAYS = 365;

    private static final int DISTRACTOR_OPTIONS = 3;

    private final WordMapper wordMapper;
    private final ProgressMapper progressMapper;
    private final UserBookMapper userBookMapper;
    private final VocabularyService vocabularyService;
    private final Clock clock;

    public StudyService(
            WordMapper wordMapper,
            ProgressMapper progressMapper,
            UserBookMapper userBookMapper,
            VocabularyService vocabularyService,
            Clock clock
    ) {
        this.wordMapper = wordMapper;
        this.progressMapper = progressMapper;
        this.userBookMapper = userBookMapper;
        this.vocabularyService = vocabularyService;
        this.clock = clock;
    }

    public List<WordCardResponse> queue(long bookId, long userId, int requestedLimit) {
        vocabularyService.getBook(bookId, userId);
        int limit = Math.min(100, Math.max(1, requestedLimit));
        LocalDateTime now = LocalDateTime.now(clock);

        List<WordCardResponse> result = new ArrayList<>(limit);
        wordMapper.findDueWords(bookId, userId, now, limit)
                .stream()
                .map(WordCardResponse::from)
                .forEach(result::add);

        int remaining = limit - result.size();
        if (remaining > 0) {
            boolean immersive = isImmersive(userId);
            wordMapper.findNewWords(bookId, userId, remaining)
                    .stream()
                    .map(row -> immersive
                            ? WordCardResponse.from(row, distractors(bookId, row))
                            : WordCardResponse.from(row))
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    private boolean isImmersive(long userId) {
        String mode = userBookMapper.findLearningMode(userId);
        return mode != null && "IMMERSIVE".equalsIgnoreCase(mode.trim());
    }

    private List<MeaningOption> distractors(long bookId, WordRow row) {
        return wordMapper.findDistractors(bookId, row.getId(), row.getWord(), DISTRACTOR_OPTIONS)
                .stream()
                .map(MeaningOption::from)
                .toList();
    }

    @Transactional
    public StudyReviewResponse review(long userId, StudyReviewRequest request) {
        vocabularyService.getWord(request.wordId(), userId);
        String clientReviewId = request.clientReviewId() == null || request.clientReviewId().isBlank()
                ? UUID.randomUUID().toString()
                : request.clientReviewId().trim();

        Long loggedWordId = progressMapper.findLoggedWordId(userId, clientReviewId);
        if (loggedWordId != null) {
            if (!loggedWordId.equals(request.wordId())) {
                throw new ConflictException("复习请求编号已用于其他单词");
            }
            UserWordProgressRow existing = progressMapper.findByUserAndWord(userId, request.wordId());
            return response(existing, request.rating());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        UserWordProgressRow progress = progressMapper.findByUserAndWord(userId, request.wordId());
        boolean isNew = progress == null;
        if (isNew) {
            progress = newProgress(userId, request.wordId(), now);
        }

        applyRating(progress, request.rating(), now);
        int changed = isNew ? progressMapper.insertProgress(progress) : progressMapper.updateProgress(progress);
        if (changed != 1) {
            throw new ConflictException("单词进度已发生变化");
        }

        long responseMs = request.responseMs() == null ? 0L : Math.min(request.responseMs(), 3_600_000L);
        progressMapper.insertReviewLog(
                clientReviewId,
                userId,
                request.wordId(),
                request.sessionId(),
                request.rating().name(),
                responseMs,
                now
        );
        return response(progress, request.rating());
    }

    private UserWordProgressRow newProgress(long userId, long wordId, LocalDateTime now) {
        UserWordProgressRow progress = new UserWordProgressRow();
        progress.setUserId(userId);
        progress.setWordId(wordId);
        progress.setStatus(ProgressStatus.NEW.name());
        progress.setEaseFactor(2.5);
        progress.setIntervalDays(0);
        progress.setLastIntervalDays(0);
        progress.setRepetitions(0);
        progress.setNextReviewAt(now);
        progress.setLastReviewedAt(now);
        progress.setCorrectCount(0);
        progress.setWrongCount(0);
        progress.setVersion(0L);
        return progress;
    }

    private void applyRating(UserWordProgressRow progress, ReviewRating rating, LocalDateTime now) {
        int oldRepetitions = value(progress.getRepetitions());
        double oldEase = progress.getEaseFactor() == null ? 2.5 : progress.getEaseFactor();
        int oldLastInterval = value(progress.getLastIntervalDays());
        boolean mastered = ProgressStatus.MASTERED.name().equals(progress.getStatus());
        int repetitions;
        int interval;
        double ease;
        LocalDateTime nextReview;

        switch (rating) {
            case AGAIN -> {
                repetitions = 0;
                interval = 0;
                ease = clamp(oldEase - 0.20);
                nextReview = now.plusMinutes(10);
                progress.setWrongCount(value(progress.getWrongCount()) + 1);
            }
            case HARD -> {
                // HARD is still an unfinished recall: the 30-minute revisit
                // re-verifies it inside the session. It neither advances the
                // long-term interval nor keeps the old interval, so a fuzzy
                // answer cannot inflate the next schedule. The next GOOD
                // restarts the curve at one day.
                repetitions = oldRepetitions;
                interval = 0;
                ease = clamp(oldEase - 0.10);
                nextReview = now.plusMinutes(30);
                progress.setLastIntervalDays(0);
                progress.setWrongCount(value(progress.getWrongCount()) + 1);
            }
            case GOOD -> {
                repetitions = oldRepetitions + 1;
                interval = sm2Interval(repetitions, oldLastInterval, oldEase);
                ease = clamp(oldEase);
                nextReview = now.plusDays(interval);
                progress.setCorrectCount(value(progress.getCorrectCount()) + 1);
            }
            case EASY -> {
                repetitions = oldRepetitions + 1;
                // Mastering starts at a month out; later EASY/GOOD answers
                // keep extending it along the SM-2 curve so mastered words
                // stay due and come back before they are forgotten.
                interval = Math.max(EASY_INITIAL_INTERVAL_DAYS, sm2Interval(repetitions, oldLastInterval, oldEase));
                ease = clamp(oldEase + 0.15);
                nextReview = now.plusDays(interval);
                progress.setCorrectCount(value(progress.getCorrectCount()) + 1);
            }
            default -> throw new IllegalArgumentException("Unsupported rating");
        }

        ProgressStatus status;
        status = switch (rating) {
            case AGAIN -> ProgressStatus.NEW;
            case HARD -> ProgressStatus.LEARNING;
            // A mastered word that is still remembered stays mastered;
            // forgetting it (AGAIN/HARD) downgrades it back to NEW/LEARNING.
            case GOOD -> mastered ? ProgressStatus.MASTERED : ProgressStatus.REVIEWING;
            case EASY -> ProgressStatus.MASTERED;
        };

        progress.setStatus(status.name());
        progress.setEaseFactor(ease);
        progress.setIntervalDays(interval);
        if (interval > 0) {
            progress.setLastIntervalDays(interval);
        }
        progress.setRepetitions(repetitions);
        progress.setNextReviewAt(nextReview);
        progress.setLastReviewedAt(now);
    }

    /**
     * SM-2 style interval: the first successful review schedules one day
     * out; every later success multiplies the previous successful interval
     * by the ease factor. Bounded by {@link #MAX_INTERVAL_DAYS}.
     */
    private int sm2Interval(int newRepetitions, int lastIntervalDays, double ease) {
        if (newRepetitions <= 1) {
            return FIRST_GOOD_INTERVAL_DAYS;
        }
        long scaled = Math.round(lastIntervalDays * ease);
        return Math.min(MAX_INTERVAL_DAYS, Math.max(FIRST_GOOD_INTERVAL_DAYS, (int) scaled));
    }

    private StudyReviewResponse response(UserWordProgressRow progress, ReviewRating rating) {
        if (progress == null) {
            throw new ConflictException("复习记录不存在");
        }
        String progressStatus = progress.getStatus();
        boolean repeatInSession = rating == ReviewRating.AGAIN || rating == ReviewRating.HARD;
        int repeatAfterCards = switch (rating) {
            case AGAIN, HARD -> LOW_RATING_REPEAT_AFTER_CARDS;
            case GOOD, EASY -> 0;
        };
        return new StudyReviewResponse(
                progress.getWordId(),
                progressStatus,
                progressStatus,
                rating,
                repeatInSession,
                repeatAfterCards,
                value(progress.getRepetitions()),
                value(progress.getIntervalDays()),
                progress.getNextReviewAt() == null
                        ? null
                        : progress.getNextReviewAt().atOffset(ZoneOffset.UTC)
        );
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private double clamp(double value) {
        return Math.max(MIN_EASE, Math.min(MAX_EASE, Math.round(value * 100.0) / 100.0));
    }
}
