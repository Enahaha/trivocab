package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.dto.AdminDashboardResponse;
import com.trivocab.ielts.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;
    private final CurrentUserProvider currentUser;

    public AdminDashboardController(
            AdminDashboardService adminDashboardService,
            CurrentUserProvider currentUser
    ) {
        this.adminDashboardService = adminDashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<AdminDashboardResponse> dashboard() {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminDashboardService.dashboard());
    }
}
