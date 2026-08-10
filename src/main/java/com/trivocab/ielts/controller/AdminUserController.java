package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.dto.AdminUserResponse;
import com.trivocab.ielts.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;
    private final CurrentUserProvider currentUser;

    public AdminUserController(AdminUserService adminUserService, CurrentUserProvider currentUser) {
        this.adminUserService = adminUserService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        currentUser.requireAdmin();
        return ApiResponse.ok(adminUserService.list(page, size, keyword));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable long userId) {
        currentUser.requireAdmin();
        AdminUserService.DeleteUserResult result = adminUserService.delete(currentUser.userId(), userId);
        return switch (result) {
            case DELETED -> ResponseEntity.ok(ApiResponse.ok(null, "用户已删除"));
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("用户不存在"));
            case SELF -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("不能删除当前登录的管理员账号"));
            case ADMIN -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("不能删除管理员账号"));
        };
    }
}
