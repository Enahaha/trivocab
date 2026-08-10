package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.WordRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WordMapper {
    List<WordRow> findPage(
            @Param("bookId") long bookId,
            @Param("userId") long userId,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countByBook(@Param("bookId") long bookId, @Param("keyword") String keyword);

    WordRow findById(@Param("wordId") long wordId, @Param("userId") long userId);

    List<WordRow> findDueWords(
            @Param("bookId") long bookId,
            @Param("userId") long userId,
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    List<WordRow> findNewWords(
            @Param("bookId") long bookId,
            @Param("userId") long userId,
            @Param("limit") int limit
    );
}
