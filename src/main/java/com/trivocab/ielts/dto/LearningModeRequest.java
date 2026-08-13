package com.trivocab.ielts.dto;

import jakarta.validation.constraints.NotBlank;

public record LearningModeRequest(
        @NotBlank(message = "学习方式不能为空") String learningMode
) {
}
