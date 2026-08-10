package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.dto.StudyReviewRequest;
import com.trivocab.ielts.dto.StudyReviewResponse;
import com.trivocab.ielts.dto.WordCardResponse;
import com.trivocab.ielts.service.StudyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/study")
public class StudyController {
    private final StudyService studyService;
    private final CurrentUserProvider currentUser;

    public StudyController(StudyService studyService, CurrentUserProvider currentUser) {
        this.studyService = studyService;
        this.currentUser = currentUser;
    }

    @GetMapping("/queue")
    public ApiResponse<List<WordCardResponse>> queue(
            @RequestParam(defaultValue = "1") long bookId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(studyService.queue(bookId, currentUser.userId(), limit));
    }

    @PostMapping("/reviews")
    public ApiResponse<StudyReviewResponse> review(@Valid @RequestBody StudyReviewRequest request) {
        return ApiResponse.ok(
                studyService.review(currentUser.userId(), request),
                "复习进度已保存"
        );
    }
}
