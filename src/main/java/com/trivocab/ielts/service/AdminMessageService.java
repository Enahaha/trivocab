package com.trivocab.ielts.service;

import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.domain.AdminMessageRow;
import com.trivocab.ielts.dto.AdminMessageResponse;
import com.trivocab.ielts.dto.AdminMessageUpdateRequest;
import com.trivocab.ielts.exception.ResourceNotFoundException;
import com.trivocab.ielts.mapper.AdminMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminMessageService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("NEW", "READ", "REPLIED", "CLOSED");

    private final AdminMessageMapper adminMessageMapper;

    public AdminMessageService(AdminMessageMapper adminMessageMapper) {
        this.adminMessageMapper = adminMessageMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminMessageResponse> list(
            int requestedPage,
            int requestedSize,
            String requestedStatus,
            String requestedKeyword
    ) {
        PageQuery query = pageQuery(requestedPage, requestedSize, requestedKeyword);
        String status = normalizeStatus(requestedStatus, true);
        List<AdminMessageResponse> messages = adminMessageMapper
                .findPage(status, query.keyword(), query.size(), query.offset())
                .stream()
                .map(AdminMessageResponse::from)
                .toList();
        long total = adminMessageMapper.count(status, query.keyword());
        return PageResult.of(messages, query.page(), query.size(), total);
    }

    @Transactional
    public AdminMessageResponse update(long messageId, AdminMessageUpdateRequest request) {
        requirePositiveId(messageId);
        if (request == null) {
            throw new IllegalArgumentException("更新内容不能为空");
        }
        String status = normalizeStatus(request.status(), true);
        boolean updateReply = request.adminReply() != null;
        String reply = updateReply ? normalizeReply(request.adminReply()) : null;
        if (status == null && !updateReply) {
            throw new IllegalArgumentException("状态和回复至少填写一项");
        }
        if (reply != null && (status == null || "NEW".equals(status) || "READ".equals(status))) {
            status = "REPLIED";
        }
        if (adminMessageMapper.update(messageId, status, reply, updateReply) != 1) {
            throw new ResourceNotFoundException("留言不存在");
        }
        AdminMessageRow updated = adminMessageMapper.findById(messageId);
        if (updated == null) {
            throw new ResourceNotFoundException("留言不存在");
        }
        return AdminMessageResponse.from(updated);
    }

    @Transactional
    public void delete(long messageId) {
        requirePositiveId(messageId);
        if (adminMessageMapper.delete(messageId) != 1) {
            throw new ResourceNotFoundException("留言不存在");
        }
    }

    private String normalizeStatus(String value, boolean nullable) {
        if (value == null || value.isBlank()) {
            if (nullable) {
                return null;
            }
            throw new IllegalArgumentException("留言状态不能为空");
        }
        String status = value.strip().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("留言状态不正确");
        }
        return status;
    }

    private String normalizeReply(String value) {
        String reply = value.strip();
        if (reply.length() > 2000) {
            throw new IllegalArgumentException("管理员回复不能超过2000个字符");
        }
        return reply.isEmpty() ? null : reply;
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
            throw new IllegalArgumentException("留言ID必须大于0");
        }
    }

    private record PageQuery(int page, int size, int offset, String keyword) {
    }
}
