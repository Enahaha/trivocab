package com.trivocab.ielts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminBookUpsertRequest(
        @NotBlank(message = "词书编码不能为空")
        @Size(max = 80, message = "词书编码不能超过80个字符")
        String code,

        @NotBlank(message = "词书名称不能为空")
        @Size(max = 160, message = "词书名称不能超过160个字符")
        String name,

        @Size(max = 1000, message = "词书说明不能超过1000个字符")
        String description
) {
}
