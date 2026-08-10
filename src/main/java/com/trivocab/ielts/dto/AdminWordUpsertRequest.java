package com.trivocab.ielts.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminWordUpsertRequest(
        @NotNull(message = "词书ID不能为空")
        @Positive(message = "词书ID必须大于0")
        Long bookId,

        @Size(max = 64, message = "Word ID不能超过64个字符")
        String wordId,

        @NotNull(message = "词汇优先级不能为空")
        @Positive(message = "词汇优先级必须大于0")
        @Max(value = 1_000_000, message = "词汇优先级不能超过1000000")
        Integer priorityRank,

        @NotBlank(message = "英文单词不能为空")
        @Size(max = 160, message = "英文单词不能超过160个字符")
        String word,

        @Size(max = 255, message = "音标不能超过255个字符")
        String phonetic,

        @Size(max = 120, message = "词性不能超过120个字符")
        String partOfSpeech,

        @NotBlank(message = "中文释义不能为空")
        @Size(max = 5000, message = "中文释义不能超过5000个字符")
        String chineseMeaning,

        @NotBlank(message = "韩文释义不能为空")
        @Size(max = 5000, message = "韩文释义不能超过5000个字符")
        String koreanMeaning,

        @Size(max = 5000, message = "韩文近义表达不能超过5000个字符")
        String koreanEquivalents,

        @Size(max = 5000, message = "韩文定义不能超过5000个字符")
        String koreanDefinition,

        @Size(max = 40, message = "韩语来源标记不能超过40个字符")
        String koreanSourceFlag,

        @Size(max = 5000, message = "英文例句不能超过5000个字符")
        String englishExample,

        @Size(max = 5000, message = "韩文例句不能超过5000个字符")
        String koreanExample,

        @Size(max = 120, message = "学习阶段不能超过120个字符")
        String learningStage,

        @Size(max = 255, message = "选词依据不能超过255个字符")
        String selectionBasis,

        @Size(max = 255, message = "来源名称不能超过255个字符")
        String sourceName,

        @Size(max = 2000, message = "来源链接不能超过2000个字符")
        String sourceUrl
) {
}
