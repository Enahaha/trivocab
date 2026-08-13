package com.trivocab.ielts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettingsUpdateRequest(
        @NotBlank(message = "学习方式不能为空")
        @Size(max = 16, message = "学习方式长度超出限制")
        String learningMode,

        boolean spellingEnabled,

        @NotBlank(message = "释义展示方式不能为空")
        @Size(max = 16, message = "释义展示方式长度超出限制")
        String meaningDisplay,

        @NotBlank(message = "主题不能为空")
        @Size(max = 16, message = "主题长度超出限制")
        String theme
) {
}
