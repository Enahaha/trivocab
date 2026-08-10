package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.UserBookSettingsRow;
import org.apache.ibatis.annotations.Param;

public interface UserBookMapper {
    Long findSelectedBookId(@Param("userId") long userId);

    int findDefaultDailyGoal(@Param("userId") long userId);

    boolean userExists(@Param("userId") long userId);

    int updateSelectedBook(@Param("userId") long userId, @Param("bookId") long bookId);

    UserBookSettingsRow findSetting(@Param("userId") long userId, @Param("bookId") long bookId);

    int insertSetting(UserBookSettingsRow row);

    int updateSettingDailyGoal(
            @Param("userId") long userId,
            @Param("bookId") long bookId,
            @Param("dailyGoal") int dailyGoal
    );

    int deleteByBook(@Param("bookId") long bookId);
}
