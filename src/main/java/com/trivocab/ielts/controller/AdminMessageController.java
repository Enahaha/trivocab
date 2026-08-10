package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.dto.AdminMessageResponse;
import com.trivocab.ielts.dto.AdminMessageUpdateRequest;
import com.trivocab.ielts.service.AdminMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/messages")
public class AdminMessageController {
    private final AdminMessageService adminMessageService;
    private final CurrentUserProvider currentUser;

    public AdminMessageController(
            AdminMessageService adminMessageService,
            CurrentUserProvider currentUser
    ) {
        this.adminMessageService = adminMessageService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminMessageResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminMessageService.list(page, size, status, keyword));
    }

    @PatchMapping("/{messageId}")
    public ApiResponse<AdminMessageResponse> update(
            @PathVariable long messageId,
            @Valid @RequestBody AdminMessageUpdateRequest request
    ) {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminMessageService.update(messageId, request), "留言已更新");
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> delete(@PathVariable long messageId) {
        currentUser.requireAdmin();
        adminMessageService.delete(messageId);
        return ApiResponse.ok(null, "留言已删除");
    }
}
