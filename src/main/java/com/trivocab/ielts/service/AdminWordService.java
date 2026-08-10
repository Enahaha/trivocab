package com.trivocab.ielts.service;

import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.domain.AdminWordRow;
import com.trivocab.ielts.dto.AdminWordResponse;
import com.trivocab.ielts.dto.AdminWordUpsertRequest;
import com.trivocab.ielts.exception.ResourceNotFoundException;
import com.trivocab.ielts.mapper.AdminWordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminWordService {
    private final AdminWordMapper adminWordMapper;

    public AdminWordService(AdminWordMapper adminWordMapper) {
        this.adminWordMapper = adminWordMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminWordResponse> list(
            Long requestedBookId,
            int requestedPage,
            int requestedSize,
            String requestedKeyword
    ) {
        if (requestedBookId != null && requestedBookId <= 0) {
            throw new IllegalArgumentException("词书ID必须大于0");
        }
        PageQuery query = pageQuery(requestedPage, requestedSize, requestedKeyword);
        List<AdminWordResponse> words = adminWordMapper
                .findPage(requestedBookId, query.keyword(), query.size(), query.offset())
                .stream()
                .map(AdminWordResponse::from)
                .toList();
        long total = adminWordMapper.count(requestedBookId, query.keyword());
        return PageResult.of(words, query.page(), query.size(), total);
    }

    @Transactional
    public AdminWordResponse create(AdminWordUpsertRequest request) {
        AdminWordRow row = toRow(null, request);
        requireBook(row.getBookId());
        if (adminWordMapper.insert(row) != 1 || row.getId() == null) {
            throw new IllegalStateException("单词保存失败");
        }
        adminWordMapper.updateBookTotal(row.getBookId());
        return getRequired(row.getId());
    }

    @Transactional
    public AdminWordResponse update(long wordId, AdminWordUpsertRequest request) {
        requirePositiveId(wordId, "单词ID必须大于0");
        AdminWordRow existing = adminWordMapper.findById(wordId);
        if (existing == null) {
            throw new ResourceNotFoundException("单词不存在");
        }
        AdminWordRow row = toRow(wordId, request);
        requireBook(row.getBookId());
        if (adminWordMapper.update(row) != 1) {
            throw new ResourceNotFoundException("单词不存在");
        }
        adminWordMapper.updateBookTotal(existing.getBookId());
        if (!existing.getBookId().equals(row.getBookId())) {
            adminWordMapper.updateBookTotal(row.getBookId());
        }
        return getRequired(wordId);
    }

    @Transactional
    public void delete(long wordId) {
        requirePositiveId(wordId, "单词ID必须大于0");
        AdminWordRow existing = adminWordMapper.findById(wordId);
        if (existing == null) {
            throw new ResourceNotFoundException("单词不存在");
        }
        adminWordMapper.deleteReviewLogs(wordId);
        adminWordMapper.deleteProgress(wordId);
        if (adminWordMapper.deleteWord(wordId) != 1) {
            throw new ResourceNotFoundException("单词不存在");
        }
        adminWordMapper.updateBookTotal(existing.getBookId());
    }

    private AdminWordResponse getRequired(long wordId) {
        AdminWordRow saved = adminWordMapper.findById(wordId);
        if (saved == null) {
            throw new ResourceNotFoundException("单词不存在");
        }
        return AdminWordResponse.from(saved);
    }

    private void requireBook(long bookId) {
        if (!adminWordMapper.bookExists(bookId)) {
            throw new ResourceNotFoundException("词书不存在");
        }
    }

    private AdminWordRow toRow(Long id, AdminWordUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("单词内容不能为空");
        }
        long bookId = requirePositiveId(request.bookId(), "词书ID必须大于0");
        int priorityRank = requirePositiveInt(request.priorityRank(), "词汇优先级必须大于0");
        if (priorityRank > 1_000_000) {
            throw new IllegalArgumentException("词汇优先级不能超过1000000");
        }

        AdminWordRow row = new AdminWordRow();
        row.setId(id);
        row.setBookId(bookId);
        row.setPriorityRank(priorityRank);
        row.setWordId(optional(request.wordId(), 64, "Word ID"));
        if (row.getWordId() == null) {
            row.setWordId(defaultWordId(bookId, priorityRank));
        }
        row.setWord(required(request.word(), 160, "英文单词"));
        row.setPhonetic(optional(request.phonetic(), 255, "音标"));
        row.setPartOfSpeech(optional(request.partOfSpeech(), 120, "词性"));
        row.setChineseMeaning(required(request.chineseMeaning(), 5000, "中文释义"));
        row.setKoreanMeaning(required(request.koreanMeaning(), 5000, "韩文释义"));
        row.setKoreanEquivalents(optional(request.koreanEquivalents(), 5000, "韩文近义表达"));
        row.setKoreanDefinition(optional(request.koreanDefinition(), 5000, "韩文定义"));
        row.setKoreanSourceFlag(optional(request.koreanSourceFlag(), 40, "韩语来源标记"));
        row.setEnglishExample(optional(request.englishExample(), 5000, "英文例句"));
        row.setKoreanExample(optional(request.koreanExample(), 5000, "韩文例句"));
        row.setLearningStage(optional(request.learningStage(), 120, "学习阶段"));
        row.setSelectionBasis(optional(request.selectionBasis(), 255, "选词依据"));
        row.setSourceName(optional(request.sourceName(), 255, "来源名称"));
        row.setSourceUrl(optional(request.sourceUrl(), 2000, "来源链接"));
        return row;
    }

    private String required(String value, int max, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        String normalized = value.strip();
        if (normalized.length() > max) {
            throw new IllegalArgumentException(fieldName + "长度超出限制");
        }
        return normalized;
    }

    private String optional(String value, int max, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > max) {
            throw new IllegalArgumentException(fieldName + "长度超出限制");
        }
        return normalized;
    }

    private long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private void requirePositiveId(long value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private int requirePositiveInt(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
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

    private String defaultWordId(long bookId, int priorityRank) {
        String code = adminWordMapper.bookCode(bookId);
        String prefix = code == null
                ? (bookId == 1L ? "IELTS" : "BOOK" + bookId)
                : code.toUpperCase().replaceAll("[^A-Z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (prefix.isBlank()) {
            prefix = "BOOK" + bookId;
        }
        return prefix + "-" + String.format("%04d", priorityRank);
    }

    private record PageQuery(int page, int size, int offset, String keyword) {
    }
}
