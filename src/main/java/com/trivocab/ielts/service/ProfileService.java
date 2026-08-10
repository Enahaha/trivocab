package com.trivocab.ielts.service;

import com.trivocab.ielts.dto.DailyGoalResponse;
import com.trivocab.ielts.domain.UserBookSettingsRow;
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

    private void validateDailyGoal(int dailyGoal) {
        if (dailyGoal < 10 || dailyGoal > 100 || dailyGoal % 10 != 0) {
            throw new IllegalArgumentException("每日计划必须是 10 到 100 之间且以 10 为步长");
        }
    }
}
