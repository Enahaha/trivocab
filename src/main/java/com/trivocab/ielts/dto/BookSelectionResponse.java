package com.trivocab.ielts.dto;

import java.util.List;

public record BookSelectionResponse(
        Long selectedBookId,
        int defaultDailyGoal,
        List<BookSelectionItem> books
) {
}
