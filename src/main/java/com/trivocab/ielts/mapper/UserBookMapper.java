package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.UserBookSettingsRow;
import org.apache.ibatis.annotations.Param;

public interface UserBookMapper {
    Long findSelectedBookId(@Param("userId") long userId);

    int findDefaultDailyGoal(@Param("userId") long userId);

    String findLearningMode(@Param("userId") long userId);

    int updateLearningMode(@Param("userId") long userId, @Param("learningMode") String learningMode);

    boolean findSpellingEnabled(@Param("userId") long userId);

    String findMeaningDisplay(@Param("userId") long userId);

    String findTheme(@Param("userId") long userId);

    int updateUserSettings(
            @Param("userId") long userId,
            @Param("learningMode") String learningMode,
            @Param("spellingEnabled") boolean spellingEnabled,
            @Param("meaningDisplay") String meaningDisplay,
            @Param("theme") String theme
    );

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
