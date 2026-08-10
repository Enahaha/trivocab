package com.trivocab.ielts.service;

import com.trivocab.ielts.domain.AdminBookRow;
import com.trivocab.ielts.dto.AdminBookResponse;
import com.trivocab.ielts.dto.AdminBookUpsertRequest;
import com.trivocab.ielts.exception.ConflictException;
import com.trivocab.ielts.exception.ResourceNotFoundException;
import com.trivocab.ielts.mapper.AdminBookMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AdminBookService {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final AdminBookMapper adminBookMapper;

    public AdminBookService(AdminBookMapper adminBookMapper) {
        this.adminBookMapper = adminBookMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminBookResponse> list() {
        return adminBookMapper.findAll().stream().map(AdminBookResponse::from).toList();
    }

    @Transactional
    public AdminBookResponse create(AdminBookUpsertRequest request) {
        AdminBookRow row = toRow(null, request);
        if (adminBookMapper.findByCode(row.getCode()) != null) {
            throw new ConflictException("词书编码已存在");
        }
        if (adminBookMapper.insert(row) != 1 || row.getId() == null) {
            throw new IllegalStateException("词书保存失败");
        }
        return getRequired(row.getId());
    }

    @Transactional
    public AdminBookResponse update(long bookId, AdminBookUpsertRequest request) {
        if (bookId <= 0) {
            throw new IllegalArgumentException("词书ID必须大于0");
        }
        AdminBookRow existing = adminBookMapper.findById(bookId);
        if (existing == null) {
            throw new ResourceNotFoundException("词书不存在");
        }
        AdminBookRow row = toRow(bookId, request);
        AdminBookRow codeOwner = adminBookMapper.findByCode(row.getCode());
        if (codeOwner != null && codeOwner.getId() != bookId) {
            throw new ConflictException("词书编码已存在");
        }
        if (adminBookMapper.update(row) != 1) {
            throw new ResourceNotFoundException("词书不存在");
        }
        return getRequired(bookId);
    }

    @Transactional
    public void delete(long bookId) {
        if (bookId <= 0) {
            throw new IllegalArgumentException("词书ID必须大于0");
        }
        AdminBookRow existing = adminBookMapper.findById(bookId);
        if (existing == null) {
            throw new ResourceNotFoundException("词书不存在");
        }
        if (adminBookMapper.findAll().size() <= 1) {
            throw new ConflictException("至少需要保留一本词书");
        }
        adminBookMapper.deleteReviewLogsByBook(bookId);
        adminBookMapper.deleteProgressByBook(bookId);
        adminBookMapper.deleteSessionsByBook(bookId);
        adminBookMapper.deleteUserSettingsByBook(bookId);
        adminBookMapper.clearSelectedBookRefs(bookId);
        adminBookMapper.deleteWordsByBook(bookId);
        if (adminBookMapper.delete(bookId) != 1) {
            throw new ResourceNotFoundException("词书不存在");
        }
    }

    private AdminBookRow toRow(Long id, AdminBookUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("词书内容不能为空");
        }
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("词书编码只能包含字母、数字、下划线或短横线");
        }
        String name = request.name().strip();
        String description = request.description() == null || request.description().isBlank()
                ? null
                : request.description().strip();
        AdminBookRow row = new AdminBookRow();
        row.setId(id);
        row.setCode(code);
        row.setName(name);
        row.setDescription(description);
        return row;
    }

    private AdminBookResponse getRequired(long bookId) {
        AdminBookRow saved = adminBookMapper.findById(bookId);
        if (saved == null) {
            throw new ResourceNotFoundException("词书不存在");
        }
        return AdminBookResponse.from(saved);
    }
}
