package com.trivocab.ielts.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminMessageUpdateRequest(
        @Pattern(
                regexp = "(?i)NEW|READ|REPLIED|CLOSED",
                message = "状态只能是NEW、READ、REPLIED或CLOSED"
        )
        String status,

        @Size(max = 2000, message = "管理员回复不能超过2000个字符")
        String adminReply
) {
}
