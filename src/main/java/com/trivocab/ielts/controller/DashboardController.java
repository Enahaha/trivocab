package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.dto.DashboardResponse;
import com.trivocab.ielts.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUser;

    public DashboardController(DashboardService dashboardService, CurrentUserProvider currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<DashboardResponse> dashboard(@RequestParam(defaultValue = "1") long bookId) {
        return ApiResponse.ok(dashboardService.dashboard(bookId, currentUser.userId()));
    }
}
