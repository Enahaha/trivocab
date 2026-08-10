package com.trivocab.ielts;

import com.trivocab.ielts.common.AuthSession;
import com.trivocab.ielts.mapper.UserBookMapper;
import com.trivocab.ielts.service.DashboardService;
import com.trivocab.ielts.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-daily-goal-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.auth.allow-demo-user=false"
})
@Import(DailyGoalPlanTests.FixedClockConfiguration.class)
class DailyGoalPlanTests {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private MockHttpSession session;
    private String csrfToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Enahaha\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        session = (MockHttpSession) login.getRequest().getSession(false);
        csrfToken = (String) session.getAttribute(AuthSession.CSRF_TOKEN);
    }

    @Test
    void persistsThePlanAndReturnsTheSameEstimateFromProfileAndDashboard() throws Exception {
        String expectedDate = TODAY.plusDays(99).toString();

        mockMvc.perform(patch("/api/v1/profile/daily-goal")
                        .session(session)
                        .header("X-CSRF-Token", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyGoal\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyGoal").value(30))
                .andExpect(jsonPath("$.data.bookId").value(1))
                .andExpect(jsonPath("$.data.remainingWords").value(3000))
                .andExpect(jsonPath("$.data.estimatedDays").value(100))
                .andExpect(jsonPath("$.data.estimatedCompletionDate").value(expectedDate));

        Long userId = ((Number) session.getAttribute(AuthSession.USER_ID)).longValue();
        Integer storedGoal = jdbcTemplate.queryForObject(
                "SELECT daily_goal FROM user_book_settings WHERE user_id = ? AND book_id = 1",
                Integer.class,
                userId
        );
        assertThat(storedGoal).isEqualTo(30);

        mockMvc.perform(get("/api/v1/dashboard")
                        .session(session)
                        .param("bookId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyGoal").value(30))
                .andExpect(jsonPath("$.data.remainingWords").value(3000))
                .andExpect(jsonPath("$.data.estimatedDays").value(100))
                .andExpect(jsonPath("$.data.estimatedCompletionDate").value(expectedDate));
    }

    @Test
    void acceptsBothBoundariesAndUsesCeilingDivision() throws Exception {
        updateGoal(10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedDays").value(300))
                .andExpect(jsonPath("$.data.estimatedCompletionDate")
                        .value(TODAY.plusDays(299).toString()));

        updateGoal(100)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedDays").value(30))
                .andExpect(jsonPath("$.data.estimatedCompletionDate")
                        .value(TODAY.plusDays(29).toString()));
    }

    @Test
    void rejectsValuesOutsideTheRangeOrNotOnATenPointStep() throws Exception {
        for (int invalidGoal : new int[]{1, 9, 25, 101, 110}) {
            updateGoal(invalidGoal).andExpect(status().isBadRequest());
        }
    }

    @Test
    void serviceValidationCannotBeBypassedByCallingItDirectly() {
        UserBookMapper userBookMapper = mock(UserBookMapper.class);
        DashboardService dashboardService = mock(DashboardService.class);
        ProfileService profileService = new ProfileService(userBookMapper, dashboardService);

        assertThatThrownBy(() -> profileService.updateDailyGoal(1L, 1L, 25))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userBookMapper, dashboardService);
    }

    private org.springframework.test.web.servlet.ResultActions updateGoal(int dailyGoal) throws Exception {
        return mockMvc.perform(patch("/api/v1/profile/daily-goal")
                .session(session)
                .header("X-CSRF-Token", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dailyGoal\":" + dailyGoal + "}"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedDailyGoalClock() {
            return Clock.fixed(Instant.parse("2026-08-09T15:30:00Z"), ZoneOffset.UTC);
        }
    }
}
