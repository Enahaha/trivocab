package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.AdminMessageRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminMessageMapper {
    List<AdminMessageRow> findPage(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long count(
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    AdminMessageRow findById(@Param("messageId") long messageId);

    int update(
            @Param("messageId") long messageId,
            @Param("status") String status,
            @Param("adminReply") String adminReply,
            @Param("updateReply") boolean updateReply
    );

    int delete(@Param("messageId") long messageId);
}
