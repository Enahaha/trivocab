package com.trivocab.ielts;

import com.trivocab.ielts.common.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-admin-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.auth.allow-demo-user=false"
})
@Transactional
class AdminApiIntegrationTests {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void coversOwnMessagesDashboardAndAdministratorCrud() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"integration_admin_user",
                                  "displayName":"集成测试用户",
                                  "email":"integration_admin_user@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession userSession = session(registration);
        String userCsrf = csrf(userSession);

        mockMvc.perform(post("/api/v1/messages")
                        .session(userSession)
                        .header("X-CSRF-Token", userCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"这是用于管理员集成测试的留言。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NEW"));
        mockMvc.perform(get("/api/v1/messages").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].content").value("这是用于管理员集成测试的留言。"));

        Long messageId = jdbcTemplate.queryForObject(
                "SELECT id FROM messages WHERE content = ?",
                Long.class,
                "这是用于管理员集成测试的留言。"
        );
        Long targetUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'integration_admin_user'",
                Long.class
        );

        jdbcTemplate.update("""
                INSERT INTO users (username, display_name, email, role, enabled, daily_goal)
                VALUES ('review_only_user', '仅学习用户', 'review_only_user@example.com', 'USER', TRUE, 20)
                """);
        Long reviewOnlyUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'review_only_user'",
                Long.class
        );
        jdbcTemplate.update("""
                INSERT INTO review_logs (
                    client_review_id, user_id, word_id, rating, response_ms, reviewed_at
                ) VALUES (?, ?, 1, 'GOOD', 500, ?)
                """, "admin-active-review-only", reviewOnlyUserId, LocalDateTime.now(Clock.systemUTC()));

        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Enahaha\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession adminSession = session(adminLogin);
        String adminCsrf = csrf(adminSession);

        mockMvc.perform(get("/api/v1/admin/dashboard").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").isNotEmpty())
                .andExpect(jsonPath("$.data.todayActiveUsers").value(3))
                .andExpect(jsonPath("$.data.todayLoginCount").value(2))
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalWords").value(6611));

        mockMvc.perform(get("/api/v1/admin/messages")
                        .session(adminSession)
                        .param("status", "NEW")
                        .param("keyword", "管理员集成测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("integration_admin_user"));
        mockMvc.perform(patch("/api/v1/admin/messages/{messageId}", messageId)
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminReply\":\"已记录，谢谢反馈。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REPLIED"));
        mockMvc.perform(delete("/api/v1/admin/messages/{messageId}", messageId)
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/users")
                        .session(adminSession)
                        .param("keyword", "integration_admin_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].role").value("USER"));

        mockMvc.perform(post("/api/v1/admin/words")
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId":1,
                                  "wordId":"IELTS-990001",
                                  "priorityRank":990001,
                                  "word":"integrationword",
                                  "partOfSpeech":"n.",
                                  "chineseMeaning":"集成测试词",
                                  "koreanMeaning":"통합 테스트 단어",
                                  "englishExample":"This word verifies the administrator API.",
                                  "koreanExample":"이 단어는 관리자 API를 검증한다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.word").value("integrationword"))
                .andExpect(jsonPath("$.data.wordId").value("IELTS-990001"));
        Long wordId = jdbcTemplate.queryForObject(
                "SELECT id FROM words WHERE book_id = 1 AND word = 'integrationword'",
                Long.class
        );
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_words FROM word_books WHERE id = 1", Integer.class
        )).isEqualTo(3001);

        mockMvc.perform(put("/api/v1/admin/words/{wordId}", wordId)
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId":1,
                                  "wordId":"IELTS-990001",
                                  "priorityRank":990001,
                                  "word":"integrationword",
                                  "partOfSpeech":"n.",
                                  "chineseMeaning":"更新后的集成测试词",
                                  "koreanMeaning":"수정된 통합 테스트 단어"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chineseMeaning").value("更新后的集成测试词"));
        mockMvc.perform(delete("/api/v1/admin/words/{wordId}", wordId)
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_words FROM word_books WHERE id = 1", Integer.class
        )).isEqualTo(3000);

        mockMvc.perform(delete("/api/v1/admin/users/{userId}", targetUserId)
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, targetUserId
        )).isZero();
    }

    private MockHttpSession session(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) {
        return (String) session.getAttribute(AuthSession.CSRF_TOKEN);
    }
}
