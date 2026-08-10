package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.dto.AdminBookResponse;
import com.trivocab.ielts.dto.AdminBookUpsertRequest;
import com.trivocab.ielts.service.AdminBookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/books")
public class AdminBookController {
    private final AdminBookService adminBookService;
    private final CurrentUserProvider currentUser;

    public AdminBookController(AdminBookService adminBookService, CurrentUserProvider currentUser) {
        this.adminBookService = adminBookService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<List<AdminBookResponse>> list() {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminBookService.list());
    }

    @PostMapping
    public ApiResponse<AdminBookResponse> create(
            @Valid @RequestBody AdminBookUpsertRequest request
    ) {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminBookService.create(request), "词书已新增");
    }

    @PutMapping("/{bookId}")
    public ApiResponse<AdminBookResponse> update(
            @PathVariable long bookId,
            @Valid @RequestBody AdminBookUpsertRequest request
    ) {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminBookService.update(bookId, request), "词书已更新");
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> delete(@PathVariable long bookId) {
        currentUser.requireAdmin();
        adminBookService.delete(bookId);
        return ApiResponse.ok(null, "词书已删除");
    }
}
