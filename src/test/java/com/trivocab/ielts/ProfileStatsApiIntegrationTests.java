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
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-stats-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.auth.allow-demo-user=false"
})
class ProfileStatsApiIntegrationTests {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

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
    void aggregatesDailyAndTotalStudyStatsAndSupportsCheckins() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"stats_user",
                                  "displayName":"统计用户",
                                  "email":"stats_user@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession session = session(registration);
        String csrf = csrf(session);

        Long word1 = jdbcTemplate.queryForObject(
                "SELECT id FROM words WHERE book_id = 1 ORDER BY priority_rank LIMIT 1", Long.class
        );
        Long word2 = jdbcTemplate.queryForObject(
                "SELECT id FROM words WHERE book_id = 1 ORDER BY priority_rank LIMIT 1 OFFSET 1", Long.class
        );

        review(session, csrf, word1, "stats-review-1", "GOOD", 60000);
        review(session, csrf, word2, "stats-review-2", "GOOD", 60000);
        review(session, csrf, word1, "stats-review-3", "GOOD", 60000);

        mockMvc.perform(get("/api/v1/profile/stats").session(session).param("range", "week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days.length()").value(7))
                .andExpect(jsonPath("$.data.days[6].learnedWords").value(2))
                .andExpect(jsonPath("$.data.days[6].reviewedWords").value(1))
                .andExpect(jsonPath("$.data.days[6].studyMinutes").value(3))
                .andExpect(jsonPath("$.data.summary.totalLearnedWords").value(2))
                .andExpect(jsonPath("$.data.summary.totalReviewedWords").value(1))
                .andExpect(jsonPath("$.data.summary.totalStudyMinutes").value(3))
                .andExpect(jsonPath("$.data.summary.todayLearnedWords").value(2))
                .andExpect(jsonPath("$.data.summary.todayReviewedWords").value(1))
                .andExpect(jsonPath("$.data.summary.todayCheckedIn").value(false));

        mockMvc.perform(post("/api/v1/profile/checkin")
                        .session(session)
                        .header("X-CSRF-Token", csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayCheckedIn").value(true))
                .andExpect(jsonPath("$.data.streak").value(1))
                .andExpect(jsonPath("$.data.totalCheckins").value(1));

        // Idempotent: a second check-in on the same day must not add another row.
        mockMvc.perform(post("/api/v1/profile/checkin")
                        .session(session)
                        .header("X-CSRF-Token", csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCheckins").value(1));

        LocalDate today = LocalDate.now(SEOUL);
        jdbcTemplate.update(
                "INSERT INTO checkins (user_id, checkin_date) VALUES (?, ?)",
                userId(session),
                today.minusDays(1)
        );
        assertThat(jdbcTemplate.queryForList(
                "SELECT checkin_date FROM checkins WHERE user_id = ? ORDER BY checkin_date",
                LocalDate.class,
                userId(session)
        )).containsExactly(today.minusDays(1), today);

        mockMvc.perform(get("/api/v1/profile/checkins")
                        .session(session)
                        .param("year", String.valueOf(today.getYear()))
                        .param("month", String.valueOf(today.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.streak").value(2))
                .andExpect(jsonPath("$.data.totalCheckins").value(2))
                .andExpect(jsonPath("$.data.dates.length()").value(2));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkins WHERE user_id = ?", Integer.class, userId(session)
        )).isEqualTo(2);
    }

    private void review(
            MockHttpSession session,
            String csrf,
            long wordId,
            String clientReviewId,
            String rating,
            long responseMs
    ) throws Exception {
        mockMvc.perform(post("/api/v1/study/reviews")
                        .session(session)
                        .header("X-CSRF-Token", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "wordId":%d,
                                  "clientReviewId":"%s",
                                  "rating":"%s",
                                  "responseMs":%d
                                }
                                """.formatted(wordId, clientReviewId, rating, responseMs)))
                .andExpect(status().isOk());
    }

    private Long userId(MockHttpSession session) {
        return ((Number) session.getAttribute(AuthSession.USER_ID)).longValue();
    }

    private MockHttpSession session(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) {
        return (String) session.getAttribute(AuthSession.CSRF_TOKEN);
    }
}
