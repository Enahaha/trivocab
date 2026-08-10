package com.trivocab.ielts;

import com.trivocab.ielts.domain.AdminMessageRow;
import com.trivocab.ielts.dto.AdminMessageResponse;
import com.trivocab.ielts.dto.AdminMessageUpdateRequest;
import com.trivocab.ielts.mapper.AdminMessageMapper;
import com.trivocab.ielts.service.AdminMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminMessageServiceTests {
    private AdminMessageMapper mapper;
    private AdminMessageService service;

    @BeforeEach
    void setUp() {
        mapper = mock(AdminMessageMapper.class);
        service = new AdminMessageService(mapper);
    }

    @Test
    void addingAReplyAutomaticallyMarksTheMessageAsReplied() {
        when(mapper.update(5L, "REPLIED", "谢谢反馈", true)).thenReturn(1);
        AdminMessageRow updated = new AdminMessageRow();
        updated.setId(5L);
        updated.setUserId(2L);
        updated.setStatus("REPLIED");
        updated.setAdminReply("谢谢反馈");
        when(mapper.findById(5L)).thenReturn(updated);

        AdminMessageResponse response = service.update(
                5L,
                new AdminMessageUpdateRequest(null, "  谢谢反馈  ")
        );

        verify(mapper).update(5L, "REPLIED", "谢谢反馈", true);
        assertThat(response.status()).isEqualTo("REPLIED");
        assertThat(response.adminReply()).isEqualTo("谢谢反馈");
    }

    @Test
    void aSavedReplyOverridesANewOrReadStatusFromTheEditor() {
        when(mapper.update(6L, "REPLIED", "已经处理", true)).thenReturn(1);
        AdminMessageRow updated = new AdminMessageRow();
        updated.setId(6L);
        updated.setUserId(2L);
        updated.setStatus("REPLIED");
        updated.setAdminReply("已经处理");
        when(mapper.findById(6L)).thenReturn(updated);

        AdminMessageResponse response = service.update(
                6L,
                new AdminMessageUpdateRequest("READ", "已经处理")
        );

        verify(mapper).update(6L, "REPLIED", "已经处理", true);
        assertThat(response.status()).isEqualTo("REPLIED");
    }

    @Test
    void rejectsUnknownMessageStatus() {
        assertThatThrownBy(() -> service.list(0, 20, "DONE", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnEmptyPatch() {
        assertThatThrownBy(() -> service.update(5L, new AdminMessageUpdateRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
