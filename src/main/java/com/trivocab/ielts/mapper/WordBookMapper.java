package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.WordBookRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WordBookMapper {
    List<WordBookRow> findAll(@Param("userId") long userId);

    WordBookRow findById(@Param("bookId") long bookId, @Param("userId") long userId);
}
