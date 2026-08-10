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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-book-selection-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.auth.allow-demo-user=false"
})
@Transactional
class BookAndSelectionApiIntegrationTests {
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
    void listsBothBundledBooksAndSwitchesSelectionWithRecalculatedEstimate() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"book_selector_user",
                                  "displayName":"选书用户",
                                  "email":"book_selector_user@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession session = session(registration);
        String csrf = csrf(session);

        mockMvc.perform(get("/api/v1/books").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].totalWords").value(3000))
                .andExpect(jsonPath("$.data[1].totalWords").value(3611));

        mockMvc.perform(get("/api/v1/profile/book-selection").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedBookId").value(1))
                .andExpect(jsonPath("$.data.books.length()").value(2))
                .andExpect(jsonPath("$.data.books[0].selected").value(true))
                .andExpect(jsonPath("$.data.books[0].remainingWords").value(3000))
                .andExpect(jsonPath("$.data.books[0].estimatedDays").value(150));

        mockMvc.perform(put("/api/v1/profile/book-selection")
                        .session(session)
                        .header("X-CSRF-Token", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedBookId").value(2))
                .andExpect(jsonPath("$.data.books[1].selected").value(true))
                .andExpect(jsonPath("$.data.books[1].remainingWords").value(3611))
                .andExpect(jsonPath("$.data.books[1].estimatedDays").value(181));

        Long userId = ((Number) session.getAttribute(AuthSession.USER_ID)).longValue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT selected_book_id FROM users WHERE id = ?", Long.class, userId
        )).isEqualTo(2L);

        mockMvc.perform(get("/api/v1/dashboard").session(session).param("bookId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(2))
                .andExpect(jsonPath("$.data.totalWords").value(3611))
                .andExpect(jsonPath("$.data.remainingWords").value(3611))
                .andExpect(jsonPath("$.data.estimatedDays").value(181));
    }

    @Test
    void keepsDailyGoalPerBookAndProgressPerBook() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Enahaha\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = session(login);
        String csrf = csrf(session);
        Long userId = ((Number) session.getAttribute(AuthSession.USER_ID)).longValue();

        mockMvc.perform(patch("/api/v1/profile/daily-goal")
                        .session(session)
                        .header("X-CSRF-Token", csrf)
                        .param("bookId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyGoal\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyGoal").value(30))
                .andExpect(jsonPath("$.data.estimatedDays").value(100));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT daily_goal FROM user_book_settings WHERE user_id = ? AND book_id = 1",
                Integer.class,
                userId
        )).isEqualTo(30);

        mockMvc.perform(get("/api/v1/profile/book-selection").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].dailyGoal").value(30))
                .andExpect(jsonPath("$.data.books[1].dailyGoal").value(20));

        // Progress for book 2 is independent of book 1.
        Long book2WordId = jdbcTemplate.queryForObject(
                "SELECT id FROM words WHERE book_id = 2 ORDER BY priority_rank LIMIT 1",
                Long.class
        );
        jdbcTemplate.update("""
                INSERT INTO user_word_progress (
                    user_id, word_id, status, ease_factor, interval_days, repetitions,
                    next_review_at, last_reviewed_at, version
                ) VALUES (?, ?, 'REVIEWING', 2.5, 1, 1, ?, ?, 0)
                """, userId, book2WordId,
                java.time.LocalDateTime.now(java.time.Clock.systemUTC()),
                java.time.LocalDateTime.now(java.time.Clock.systemUTC()));

        mockMvc.perform(get("/api/v1/dashboard").session(session).param("bookId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learnedWords").value(1))
                .andExpect(jsonPath("$.data.remainingWords").value(3610))
                .andExpect(jsonPath("$.data.estimatedDays").value(181));

        mockMvc.perform(get("/api/v1/dashboard").session(session).param("bookId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learnedWords").value(0));
    }

    @Test
    void adminCanCreateUpdateAndDeleteBooksAndWordsFollowTheBook() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Enahaha\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession adminSession = session(login);
        String adminCsrf = csrf(adminSession);

        mockMvc.perform(post("/api/v1/admin/books")
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"TEST_BOOK_100",
                                  "name":"测试词书",
                                  "description":"管理员集成测试创建。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("TEST_BOOK_100"))
                .andExpect(jsonPath("$.data.totalWords").value(0));

        Long bookId = jdbcTemplate.queryForObject(
                "SELECT id FROM word_books WHERE code = 'TEST_BOOK_100'", Long.class
        );

        mockMvc.perform(put("/api/v1/admin/books/{bookId}", bookId)
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"TEST_BOOK_100",
                                  "name":"测试词书（改名）",
                                  "description":"更新后的说明。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试词书（改名）"));

        mockMvc.perform(post("/api/v1/admin/books")
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"TEST_BOOK_100",
                                  "name":"重复编码",
                                  "description":""
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/admin/words")
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId":%d,
                                  "priorityRank":1,
                                  "word":"integrationbookword",
                                  "partOfSpeech":"n.",
                                  "chineseMeaning":"测试词",
                                  "koreanMeaning":"테스트 단어",
                                  "koreanSourceFlag":"AI翻译（需人工复核）"
                                }
                                """.formatted(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wordId").value("TEST-BOOK-100-0001"))
                .andExpect(jsonPath("$.data.koreanSourceFlag").value("AI翻译（需人工复核）"));

        mockMvc.perform(get("/api/v1/admin/words")
                        .session(adminSession)
                        .param("bookId", String.valueOf(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(delete("/api/v1/admin/books/{bookId}", bookId)
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM word_books WHERE id = ?", Integer.class, bookId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM words WHERE book_id = ?", Integer.class, bookId
        )).isZero();
    }

    private MockHttpSession session(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) {
        return (String) session.getAttribute(AuthSession.CSRF_TOKEN);
    }
}
