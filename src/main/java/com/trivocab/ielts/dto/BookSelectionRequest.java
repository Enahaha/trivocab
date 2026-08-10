package com.trivocab.ielts.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookSelectionRequest(
        @NotNull(message = "词书ID不能为空")
        @Positive(message = "词书ID必须大于0")
        Long bookId
) {
}
