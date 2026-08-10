package com.trivocab.ielts;

import com.trivocab.ielts.domain.AdminWordRow;
import com.trivocab.ielts.dto.AdminWordResponse;
import com.trivocab.ielts.dto.AdminWordUpsertRequest;
import com.trivocab.ielts.mapper.AdminWordMapper;
import com.trivocab.ielts.service.AdminWordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminWordServiceTests {
    private AdminWordMapper mapper;
    private AdminWordService service;

    @BeforeEach
    void setUp() {
        mapper = mock(AdminWordMapper.class);
        service = new AdminWordService(mapper);
    }

    @Test
    void createsANormalizedWordAndRefreshesTheBookTotal() {
        when(mapper.bookExists(1L)).thenReturn(true);
        when(mapper.insert(any(AdminWordRow.class))).thenAnswer(invocation -> {
            AdminWordRow row = invocation.getArgument(0);
            row.setId(3010L);
            return 1;
        });
        when(mapper.findById(3010L)).thenReturn(word(3010L, 1L, "abandon"));

        AdminWordResponse response = service.create(request(1L, "  abandon  "));

        ArgumentCaptor<AdminWordRow> captor = ArgumentCaptor.forClass(AdminWordRow.class);
        verify(mapper).insert(captor.capture());
        verify(mapper).updateBookTotal(1L);
        assertThat(captor.getValue().getWord()).isEqualTo("abandon");
        assertThat(captor.getValue().getWordId()).isEqualTo("IELTS-3010");
        assertThat(captor.getValue().getChineseMeaning()).isEqualTo("放弃");
        assertThat(response.id()).isEqualTo(3010L);
    }

    @Test
    void deletingAWordRemovesReviewsAndProgressFirst() {
        when(mapper.findById(20L)).thenReturn(word(20L, 1L, "allocate"));
        when(mapper.deleteWord(20L)).thenReturn(1);

        service.delete(20L);

        InOrder order = inOrder(mapper);
        order.verify(mapper).findById(20L);
        order.verify(mapper).deleteReviewLogs(20L);
        order.verify(mapper).deleteProgress(20L);
        order.verify(mapper).deleteWord(20L);
        order.verify(mapper).updateBookTotal(1L);
    }

    private AdminWordUpsertRequest request(long bookId, String word) {
        return new AdminWordUpsertRequest(
                bookId,
                null,
                3010,
                word,
                "/əˈbændən/",
                "v.",
                "  放弃  ",
                "  버리다  ",
                null,
                null,
                null,
                "They abandoned the plan.",
                "그들은 계획을 포기했다.",
                "核心",
                "IELTS",
                "测试来源",
                "https://example.com"
        );
    }

    private AdminWordRow word(long id, long bookId, String word) {
        AdminWordRow row = new AdminWordRow();
        row.setId(id);
        row.setWordId("IELTS-3010");
        row.setBookId(bookId);
        row.setBookName("IELTS 核心词汇");
        row.setPriorityRank(3010);
        row.setWord(word);
        row.setChineseMeaning("放弃");
        row.setKoreanMeaning("버리다");
        return row;
    }
}
