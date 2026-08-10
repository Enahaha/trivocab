package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.ReviewStatRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface StatsMapper {
    List<ReviewStatRow> findAllReviewStats(@Param("userId") long userId);

    List<LocalDate> findCheckinDates(@Param("userId") long userId);

    boolean checkinExists(@Param("userId") long userId, @Param("date") LocalDate date);

    int insertCheckin(@Param("userId") long userId, @Param("date") LocalDate date);

    int countCheckins(@Param("userId") long userId);
}
