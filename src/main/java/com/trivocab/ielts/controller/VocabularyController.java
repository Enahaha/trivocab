package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.common.PageResult;
import com.trivocab.ielts.dto.BookSummaryResponse;
import com.trivocab.ielts.dto.WordCardResponse;
import com.trivocab.ielts.service.VocabularyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class VocabularyController {
    private final VocabularyService vocabularyService;
    private final CurrentUserProvider currentUser;

    public VocabularyController(VocabularyService vocabularyService, CurrentUserProvider currentUser) {
        this.vocabularyService = vocabularyService;
        this.currentUser = currentUser;
    }

    @GetMapping("/books")
    public ApiResponse<List<BookSummaryResponse>> books() {
        return ApiResponse.ok(vocabularyService.listBooks(currentUser.userId()));
    }

    @GetMapping("/books/{bookId}/words")
    public ApiResponse<PageResult<WordCardResponse>> words(
            @PathVariable long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(
                vocabularyService.listWords(bookId, currentUser.userId(), page, size, keyword)
        );
    }

    @GetMapping("/words/{wordId}")
    public ApiResponse<WordCardResponse> word(@PathVariable long wordId) {
        return ApiResponse.ok(vocabularyService.getWord(wordId, currentUser.userId()));
    }
}
