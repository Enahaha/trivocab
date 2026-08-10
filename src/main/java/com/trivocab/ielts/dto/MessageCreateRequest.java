package com.trivocab.ielts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageCreateRequest(
        @NotBlank(message = "留言内容不能为空")
        @Size(min = 10, max = 1000, message = "留言内容长度必须在10到1000个字符之间")
        String content
) {
}
