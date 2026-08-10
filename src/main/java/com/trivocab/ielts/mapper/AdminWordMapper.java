package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.AdminWordRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminWordMapper {
    List<AdminWordRow> findPage(
            @Param("bookId") Long bookId,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long count(
            @Param("bookId") Long bookId,
            @Param("keyword") String keyword
    );

    AdminWordRow findById(@Param("wordId") long wordId);

    boolean bookExists(@Param("bookId") long bookId);

    String bookCode(@Param("bookId") long bookId);

    int insert(AdminWordRow word);

    int update(AdminWordRow word);

    int deleteReviewLogs(@Param("wordId") long wordId);

    int deleteProgress(@Param("wordId") long wordId);

    int deleteWord(@Param("wordId") long wordId);

    int updateBookTotal(@Param("bookId") long bookId);
}
