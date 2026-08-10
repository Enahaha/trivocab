package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.AdminBookRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminBookMapper {
    List<AdminBookRow> findAll();

    AdminBookRow findById(@Param("bookId") long bookId);

    AdminBookRow findByCode(@Param("code") String code);

    int insert(AdminBookRow book);

    int update(AdminBookRow book);

    int delete(@Param("bookId") long bookId);

    int countWords(@Param("bookId") long bookId);

    int deleteReviewLogsByBook(@Param("bookId") long bookId);

    int deleteProgressByBook(@Param("bookId") long bookId);

    int deleteSessionsByBook(@Param("bookId") long bookId);

    int deleteUserSettingsByBook(@Param("bookId") long bookId);

    int clearSelectedBookRefs(@Param("bookId") long bookId);

    int deleteWordsByBook(@Param("bookId") long bookId);
}
