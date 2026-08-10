package com.trivocab.ielts;

import com.trivocab.ielts.domain.AdminUserRow;
import com.trivocab.ielts.mapper.AdminUserMapper;
import com.trivocab.ielts.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTests {
    private AdminUserMapper mapper;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        mapper = mock(AdminUserMapper.class);
        service = new AdminUserService(mapper);
    }

    @Test
    void refusesToDeleteTheCurrentAdministrator() {
        when(mapper.findById(9L)).thenReturn(user(9L, "ADMIN"));

        AdminUserService.DeleteUserResult result = service.delete(9L, 9L);

        assertThat(result).isEqualTo(AdminUserService.DeleteUserResult.SELF);
        verify(mapper, never()).deleteUser(9L);
    }

    @Test
    void refusesToDeleteAnotherAdministrator() {
        when(mapper.findById(12L)).thenReturn(user(12L, "ADMIN"));

        AdminUserService.DeleteUserResult result = service.delete(9L, 12L);

        assertThat(result).isEqualTo(AdminUserService.DeleteUserResult.ADMIN);
        verify(mapper, never()).deleteUser(12L);
    }

    @Test
    void cleansDependentRowsBeforeDeletingARegularUser() {
        when(mapper.findById(12L)).thenReturn(user(12L, "USER"));
        when(mapper.deleteUser(12L)).thenReturn(1);

        AdminUserService.DeleteUserResult result = service.delete(9L, 12L);

        assertThat(result).isEqualTo(AdminUserService.DeleteUserResult.DELETED);
        InOrder order = inOrder(mapper);
        order.verify(mapper).findById(12L);
        order.verify(mapper).deleteMessages(12L);
        order.verify(mapper).deleteLoginEvents(12L);
        order.verify(mapper).deletePasswordResetTokens(12L);
        order.verify(mapper).deleteReviewLogs(12L);
        order.verify(mapper).deleteProgress(12L);
        order.verify(mapper).deleteSessions(12L);
        order.verify(mapper).deleteUser(12L);
    }

    private AdminUserRow user(long id, String role) {
        AdminUserRow row = new AdminUserRow();
        row.setId(id);
        row.setRole(role);
        return row;
    }
}
