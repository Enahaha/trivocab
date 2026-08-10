package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.UserWordProgressRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface ProgressMapper {
    UserWordProgressRow findByUserAndWord(@Param("userId") long userId, @Param("wordId") long wordId);

    int insertProgress(UserWordProgressRow progress);

    int updateProgress(UserWordProgressRow progress);

    Long findLoggedWordId(@Param("userId") long userId, @Param("clientReviewId") String clientReviewId);

    int insertReviewLog(
            @Param("clientReviewId") String clientReviewId,
            @Param("userId") long userId,
            @Param("wordId") long wordId,
            @Param("sessionId") Long sessionId,
            @Param("rating") String rating,
            @Param("responseMs") long responseMs,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    int updateDailyGoal(@Param("userId") long userId, @Param("dailyGoal") int dailyGoal);
}
