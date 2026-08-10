package com.trivocab.ielts.common;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(items, page, size, total, totalPages);
    }
}
