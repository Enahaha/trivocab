package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.dto.AdminWordResponse;
import com.trivocab.ielts.dto.AdminWordUpsertRequest;
import com.trivocab.ielts.service.AdminWordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/words")
public class AdminWordController {
    private final AdminWordService adminWordService;
    private final CurrentUserProvider currentUser;

    public AdminWordController(AdminWordService adminWordService, CurrentUserProvider currentUser) {
        this.adminWordService = adminWordService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminWordResponse>> list(
            @RequestParam(required = false) Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminWordService.list(bookId, page, size, keyword));
    }

    @PostMapping
    public ApiResponse<AdminWordResponse> create(
            @Valid @RequestBody AdminWordUpsertRequest request
    ) {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminWordService.create(request), "单词已新增");
    }

    @PutMapping("/{wordId}")
    public ApiResponse<AdminWordResponse> update(
            @PathVariable long wordId,
            @Valid @RequestBody AdminWordUpsertRequest request
    ) {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminWordService.update(wordId, request), "单词已更新");
    }

    @DeleteMapping("/{wordId}")
    public ApiResponse<Void> delete(@PathVariable long wordId) {
        currentUser.requireAdmin();
        adminWordService.delete(wordId);
        return ApiResponse.ok(null, "单词已删除");
    }
}
