package com.trivocab.ielts.service;

import com.trivocab.ielts.domain.MessageRow;
import com.trivocab.ielts.dto.MessageResponse;
import com.trivocab.ielts.exception.ResourceNotFoundException;
import com.trivocab.ielts.mapper.MessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {
    private final MessageMapper messageMapper;

    public MessageService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> list(long userId) {
        requirePositiveId(userId);
        return messageMapper.findByUserId(userId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse create(long userId, String requestedContent) {
        requirePositiveId(userId);
        String content = normalizeContent(requestedContent);
        MessageRow row = new MessageRow();
        row.setUserId(userId);
        row.setContent(content);
        row.setStatus("NEW");
        if (messageMapper.insert(row) != 1 || row.getId() == null) {
            throw new IllegalStateException("留言保存失败");
        }
        MessageRow saved = messageMapper.findByIdAndUserId(row.getId(), userId);
        if (saved == null) {
            throw new ResourceNotFoundException("留言不存在");
        }
        return MessageResponse.from(saved);
    }

    private String normalizeContent(String value) {
        if (value == null) {
            throw new IllegalArgumentException("留言内容不能为空");
        }
        String content = value.strip();
        if (content.length() < 10 || content.length() > 1000) {
            throw new IllegalArgumentException("留言内容长度必须在10到1000个字符之间");
        }
        return content;
    }

    private void requirePositiveId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("用户ID必须大于0");
        }
    }
}
