package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.DashboardStatsRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardMapper {
    DashboardStatsRow findStats(
            @Param("bookId") long bookId,
            @Param("userId") long userId,
            @Param("now") LocalDateTime now,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd
    );

    List<LocalDateTime> findReviewTimesBefore(
            @Param("userId") long userId,
            @Param("before") LocalDateTime before
    );
}
