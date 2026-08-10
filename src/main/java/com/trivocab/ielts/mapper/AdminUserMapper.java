package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.AdminUserRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminUserMapper {
    List<AdminUserRow> findPage(
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long count(@Param("keyword") String keyword);

    AdminUserRow findById(@Param("userId") long userId);

    int deleteMessages(@Param("userId") long userId);

    int deleteLoginEvents(@Param("userId") long userId);

    int deletePasswordResetTokens(@Param("userId") long userId);

    int deleteReviewLogs(@Param("userId") long userId);

    int deleteProgress(@Param("userId") long userId);

    int deleteSessions(@Param("userId") long userId);

    int deleteUser(@Param("userId") long userId);
}
