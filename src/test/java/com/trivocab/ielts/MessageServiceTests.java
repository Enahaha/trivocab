package com.trivocab.ielts;

import com.trivocab.ielts.domain.MessageRow;
import com.trivocab.ielts.dto.MessageResponse;
import com.trivocab.ielts.mapper.MessageMapper;
import com.trivocab.ielts.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceTests {
    private MessageMapper messageMapper;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageMapper = org.mockito.Mockito.mock(MessageMapper.class);
        messageService = new MessageService(messageMapper);
    }

    @Test
    void createsTrimmedMessageForTheCurrentUser() {
        when(messageMapper.insert(any(MessageRow.class))).thenAnswer(invocation -> {
            MessageRow row = invocation.getArgument(0);
            row.setId(21L);
            return 1;
        });
        MessageRow saved = message(21L, 7L, "希望增加错词筛选功能。", "NEW");
        when(messageMapper.findByIdAndUserId(21L, 7L)).thenReturn(saved);

        MessageResponse response = messageService.create(7L, "  希望增加错词筛选功能。  ");

        ArgumentCaptor<MessageRow> captor = ArgumentCaptor.forClass(MessageRow.class);
        verify(messageMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getContent()).isEqualTo("希望增加错词筛选功能。");
        assertThat(response.id()).isEqualTo(21L);
        assertThat(response.status()).isEqualTo("NEW");
    }

    @Test
    void rejectsContentShorterThanTenCharactersAfterTrimming() {
        assertThatThrownBy(() -> messageService.create(7L, "  太短了  "))
                .isInstanceOf(IllegalArgumentException.class);

        verify(messageMapper, never()).insert(any());
    }

    private MessageRow message(long id, long userId, String content, String status) {
        MessageRow row = new MessageRow();
        row.setId(id);
        row.setUserId(userId);
        row.setContent(content);
        row.setStatus(status);
        row.setCreatedAt(LocalDateTime.of(2026, 8, 10, 9, 30));
        row.setUpdatedAt(row.getCreatedAt());
        return row;
    }
}
