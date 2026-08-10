package com.trivocab.ielts.service;

import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.domain.AdminUserRow;
import com.trivocab.ielts.dto.AdminUserResponse;
import com.trivocab.ielts.mapper.AdminUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {
    private final AdminUserMapper adminUserMapper;

    public AdminUserService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminUserResponse> list(int requestedPage, int requestedSize, String requestedKeyword) {
        PageQuery query = pageQuery(requestedPage, requestedSize, requestedKeyword);
        List<AdminUserResponse> users = adminUserMapper
                .findPage(query.keyword(), query.size(), query.offset())
                .stream()
                .map(AdminUserResponse::from)
                .toList();
        long total = adminUserMapper.count(query.keyword());
        return PageResult.of(users, query.page(), query.size(), total);
    }

    @Transactional
    public DeleteUserResult delete(long currentAdminId, long targetUserId) {
        requirePositiveId(currentAdminId);
        requirePositiveId(targetUserId);
        AdminUserRow target = adminUserMapper.findById(targetUserId);
        if (target == null) {
            return DeleteUserResult.NOT_FOUND;
        }
        if (currentAdminId == targetUserId) {
            return DeleteUserResult.SELF;
        }
        if ("ADMIN".equalsIgnoreCase(target.getRole())) {
            return DeleteUserResult.ADMIN;
        }

        adminUserMapper.deleteMessages(targetUserId);
        adminUserMapper.deleteLoginEvents(targetUserId);
        adminUserMapper.deletePasswordResetTokens(targetUserId);
        adminUserMapper.deleteReviewLogs(targetUserId);
        adminUserMapper.deleteProgress(targetUserId);
        adminUserMapper.deleteSessions(targetUserId);
        return adminUserMapper.deleteUser(targetUserId) == 1
                ? DeleteUserResult.DELETED
                : DeleteUserResult.NOT_FOUND;
    }

    private PageQuery pageQuery(int page, int size, String keyword) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能小于0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("每页数量必须在1到100之间");
        }
        String normalized = normalizeKeyword(keyword);
        return new PageQuery(page, size, checkedOffset(page, size), normalized);
    }

    private int checkedOffset(int page, int size) {
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("页码超出允许范围");
        }
        return (int) offset;
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String keyword = value.strip();
        if (keyword.length() > 80) {
            throw new IllegalArgumentException("搜索关键词不能超过80个字符");
        }
        return keyword;
    }

    private void requirePositiveId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("用户ID必须大于0");
        }
    }

    public enum DeleteUserResult {
        DELETED,
        NOT_FOUND,
        SELF,
        ADMIN
    }

    private record PageQuery(int page, int size, int offset, String keyword) {
    }
}
