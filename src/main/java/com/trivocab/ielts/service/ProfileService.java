package com.trivocab.ielts.service;

import com.trivocab.ielts.domain.LearningMode;
import com.trivocab.ielts.domain.UserBookSettingsRow;
import com.trivocab.ielts.dto.DailyGoalResponse;
import com.trivocab.ielts.dto.SettingsResponse;
import com.trivocab.ielts.dto.SettingsUpdateRequest;
import com.trivocab.ielts.exception.ResourceNotFoundException;
import com.trivocab.ielts.mapper.UserBookMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserBookMapper userBookMapper;
    private final DashboardService dashboardService;

    public ProfileService(
            UserBookMapper userBookMapper,
            DashboardService dashboardService
    ) {
        this.userBookMapper = userBookMapper;
        this.dashboardService = dashboardService;
    }

    @Transactional
    public DailyGoalResponse updateDailyGoal(long userId, long bookId, int dailyGoal) {
        validateDailyGoal(dailyGoal);
        if (!userBookMapper.userExists(userId)) {
            throw new ResourceNotFoundException("用户不存在");
        }
        if (userBookMapper.findSetting(userId, bookId) == null) {
            UserBookSettingsRow setting = new UserBookSettingsRow();
            setting.setUserId(userId);
            setting.setBookId(bookId);
            setting.setDailyGoal(dailyGoal);
            userBookMapper.insertSetting(setting);
        } else {
            userBookMapper.updateSettingDailyGoal(userId, bookId, dailyGoal);
        }
        var dashboard = dashboardService.dashboard(bookId, userId);
        return new DailyGoalResponse(
                dashboard.dailyGoal(),
                dashboard.bookId(),
                dashboard.remainingWords(),
                dashboard.estimatedDays(),
                dashboard.estimatedCompletionDate()
        );
    }

    /**
     * Retains the original service signature for callers that use the default
     * IELTS book directly.
     */
    @Transactional
    public DailyGoalResponse updateDailyGoal(long userId, int dailyGoal) {
        return updateDailyGoal(userId, 1L, dailyGoal);
    }

    /**
     * Current learning workflow ({@link LearningMode}) for the user.
     */
    public String learningMode(long userId) {
        if (!userBookMapper.userExists(userId)) {
            throw new ResourceNotFoundException("用户不存在");
        }
        String mode = userBookMapper.findLearningMode(userId);
        return mode == null || mode.isBlank() ? LearningMode.SIMPLE.name() : mode;
    }

    @Transactional
    public String updateLearningMode(long userId, String rawMode) {
        if (!userBookMapper.userExists(userId)) {
            throw new ResourceNotFoundException("用户不存在");
        }
        LearningMode mode;
        try {
            mode = LearningMode.valueOf(rawMode == null ? "" : rawMode.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("不支持的学习方式: " + rawMode);
        }
        userBookMapper.updateLearningMode(userId, mode.name());
        return mode.name();
    }

    public SettingsResponse settings(long userId) {
        if (!userBookMapper.userExists(userId)) {
            throw new ResourceNotFoundException("用户不存在");
        }
        String learningMode = userBookMapper.findLearningMode(userId);
        if (learningMode == null || learningMode.isBlank()) {
            learningMode = LearningMode.SIMPLE.name();
        }
        String meaningDisplay = userBookMapper.findMeaningDisplay(userId);
        String theme = userBookMapper.findTheme(userId);
        return new SettingsResponse(
                learningMode,
                userBookMapper.findSpellingEnabled(userId),
                meaningDisplay == null || meaningDisplay.isBlank() ? "SIMPLIFIED" : meaningDisplay,
                theme == null || theme.isBlank() ? "SYSTEM" : theme
        );
    }

    @Transactional
    public SettingsResponse updateSettings(long userId, SettingsUpdateRequest request) {
        if (!userBookMapper.userExists(userId)) {
            throw new ResourceNotFoundException("用户不存在");
        }
        LearningMode mode;
        try {
            mode = LearningMode.valueOf(request.learningMode().trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("不支持的学习方式: " + request.learningMode());
        }
        String meaningDisplay = normalizeChoice(
                request.meaningDisplay(), "SIMPLIFIED", "DETAILED", "释义展示方式"
        );
        String theme = normalizeChoice(request.theme(), "SYSTEM", "LIGHT", "DARK", "主题");
        userBookMapper.updateUserSettings(
                userId,
                mode.name(),
                request.spellingEnabled(),
                meaningDisplay,
                theme
        );
        return new SettingsResponse(mode.name(), request.spellingEnabled(), meaningDisplay, theme);
    }

    private String normalizeChoice(String value, String first, String second, String fieldName) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!normalized.equals(first) && !normalized.equals(second)) {
            throw new IllegalArgumentException("不支持的" + fieldName + ": " + value);
        }
        return normalized;
    }

    private String normalizeChoice(String value, String first, String second, String third, String fieldName) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!normalized.equals(first) && !normalized.equals(second) && !normalized.equals(third)) {
            throw new IllegalArgumentException("不支持的" + fieldName + ": " + value);
        }
        return normalized;
    }

    private void validateDailyGoal(int dailyGoal) {
        if (dailyGoal < 10 || dailyGoal > 100 || dailyGoal % 10 != 0) {
            throw new IllegalArgumentException("每日计划必须是 10 到 100 之间且以 10 为步长");
        }
    }
}
