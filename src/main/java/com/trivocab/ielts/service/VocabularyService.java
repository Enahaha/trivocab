package com.trivocab.ielts.service;

import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.domain.WordBookRow;
import com.trivocab.ielts.domain.WordRow;
import com.trivocab.ielts.dto.BookSummaryResponse;
import com.trivocab.ielts.dto.WordCardResponse;
import com.trivocab.ielts.exception.ResourceNotFoundException;
import com.trivocab.ielts.mapper.WordBookMapper;
import com.trivocab.ielts.mapper.WordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VocabularyService {
    private final WordBookMapper wordBookMapper;
    private final WordMapper wordMapper;

    public VocabularyService(WordBookMapper wordBookMapper, WordMapper wordMapper) {
        this.wordBookMapper = wordBookMapper;
        this.wordMapper = wordMapper;
    }

    public List<BookSummaryResponse> listBooks(long userId) {
        return wordBookMapper.findAll(userId).stream().map(BookSummaryResponse::from).toList();
    }

    public BookSummaryResponse getBook(long bookId, long userId) {
        WordBookRow row = wordBookMapper.findById(bookId, userId);
        if (row == null) {
            throw new ResourceNotFoundException("词书不存在");
        }
        return BookSummaryResponse.from(row);
    }

    public PageResult<WordCardResponse> listWords(
            long bookId,
            long userId,
            int requestedPage,
            int requestedSize,
            String requestedKeyword
    ) {
        getBook(bookId, userId);
        int page = Math.max(0, requestedPage);
        int size = Math.min(100, Math.max(1, requestedSize));
        String keyword = normalizeKeyword(requestedKeyword);
        int offset = Math.multiplyExact(page, size);

        List<WordCardResponse> words = wordMapper.findPage(bookId, userId, keyword, size, offset)
                .stream()
                .map(WordCardResponse::from)
                .toList();
        long total = wordMapper.countByBook(bookId, keyword);
        return PageResult.of(words, page, size, total);
    }

    public WordCardResponse getWord(long wordId, long userId) {
        WordRow row = wordMapper.findById(wordId, userId);
        if (row == null) {
            throw new ResourceNotFoundException("单词不存在");
        }
        return WordCardResponse.from(row);
    }

    private String normalizeKeyword(String value) {
        if (value == null) {
            return null;
        }
        String keyword = value.trim();
        if (keyword.isEmpty()) {
            return null;
        }
        return keyword.length() > 80 ? keyword.substring(0, 80) : keyword;
    }
}
