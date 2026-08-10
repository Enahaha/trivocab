package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.MessageRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageMapper {
    List<MessageRow> findByUserId(@Param("userId") long userId);

    MessageRow findByIdAndUserId(
            @Param("messageId") long messageId,
            @Param("userId") long userId
    );

    int insert(MessageRow message);
}
