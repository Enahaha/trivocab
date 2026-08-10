package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.dto.DailyGoalRequest;
import com.trivocab.ielts.dto.DailyGoalResponse;
import com.trivocab.ielts.dto.BookSelectionRequest;
import com.trivocab.ielts.dto.BookSelectionResponse;
import com.trivocab.ielts.dto.CheckinResponse;
import com.trivocab.ielts.dto.StudyStatsResponse;
import com.trivocab.ielts.service.BookSelectionService;
import com.trivocab.ielts.service.ProfileService;
import com.trivocab.ielts.service.ProfileStatsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final BookSelectionService bookSelectionService;
    private final ProfileStatsService profileStatsService;
    private final CurrentUserProvider currentUser;

    public ProfileController(
            ProfileService profileService,
            BookSelectionService bookSelectionService,
            ProfileStatsService profileStatsService,
            CurrentUserProvider currentUser
    ) {
        this.profileService = profileService;
        this.bookSelectionService = bookSelectionService;
        this.profileStatsService = profileStatsService;
        this.currentUser = currentUser;
    }

    @GetMapping("/book-selection")
    public ApiResponse<BookSelectionResponse> bookSelection() {
        return ApiResponse.ok(bookSelectionService.selection(currentUser.userId()));
    }

    @PutMapping("/book-selection")
    public ApiResponse<BookSelectionResponse> switchBook(
            @Valid @RequestBody BookSelectionRequest request
    ) {
        return ApiResponse.ok(
                bookSelectionService.switchBook(currentUser.userId(), request.bookId()),
                "已切换到所选词书"
        );
    }

    @GetMapping("/stats")
    public ApiResponse<StudyStatsResponse> stats(
            @RequestParam(defaultValue = "week") String range
    ) {
        return ApiResponse.ok(profileStatsService.stats(currentUser.userId(), range));
    }

    @PostMapping("/checkin")
    public ApiResponse<CheckinResponse> checkin() {
        return ApiResponse.ok(profileStatsService.checkin(currentUser.userId()), "签到成功");
    }

    @GetMapping("/checkins")
    public ApiResponse<CheckinResponse> checkins(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.ok(profileStatsService.checkins(currentUser.userId(), year, month));
    }

    @PatchMapping("/daily-goal")
    public ApiResponse<DailyGoalResponse> updateDailyGoal(
            @RequestParam(defaultValue = "1") long bookId,
            @Valid @RequestBody DailyGoalRequest request
    ) {
        return ApiResponse.ok(
                profileService.updateDailyGoal(currentUser.userId(), bookId, request.dailyGoal()),
                "每日目标已更新"
        );
    }
}
