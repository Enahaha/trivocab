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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-auth-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.auth.expose-reset-code=true",
        "app.auth.allow-demo-user=false"
})
class AuthApiIntegrationTests {
    private static final Pattern RESET_CODE_PATTERN = Pattern.compile("\\\"resetCode\\\":\\\"(\\d{6})\\\"");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void protectsAllNonPublicApisAndRegistersAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "learner_auth_1",
                                  "displayName": "Auth Learner",
                                  "email": "learner_auth_1@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("learner_auth_1"))
                .andExpect(jsonPath("$.data.email").value("learner_auth_1@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.csrfToken").isNotEmpty())
                .andReturn();

        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(AuthSession.USER_ID)).isInstanceOf(Number.class);
        assertThat(session.getAttribute(AuthSession.ROLE)).isEqualTo("USER");
        assertThat(session.getAttribute(AuthSession.CSRF_TOKEN)).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Auth Learner"));
        mockMvc.perform(get("/api/v1/books").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/anything").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void bootstrapsTheAdminWithOnlyABcryptHashAndSupportsLoginByUsername() throws Exception {
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = 'Enahaha'",
                String.class
        );
        assertThat(storedHash).startsWith("$2");
        assertThat(storedHash).doesNotContain("123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Enahaha","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.csrfToken").isNotEmpty());
    }

    @Test
    void rejectsWrongCredentialsAndRecordsBothFailedAndSuccessfulLoginEvents() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"Enahaha","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"enahaha@local.trivocab","password":"123456"}
                                """))
                .andExpect(status().isOk());

        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM login_events WHERE event_type = 'LOGIN_FAILURE' AND LOWER(username) = 'enahaha'",
                Integer.class
        );
        Integer succeeded = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM login_events WHERE event_type = 'LOGIN_SUCCESS' AND LOWER(username) = 'enahaha'",
                Integer.class
        );
        assertThat(failed).isPositive();
        assertThat(succeeded).isPositive();
    }

    @Test
    void requiresCsrfForAuthenticatedWritesAndInvalidatesTheSessionOnLogout() throws Exception {
        MvcResult login = loginAsAdmin();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        String csrfToken = (String) session.getAttribute(AuthSession.CSRF_TOKEN);

        mockMvc.perform(patch("/api/v1/profile/daily-goal")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyGoal\":30}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/profile/daily-goal")
                        .session(session)
                        .header("X-CSRF-Token", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyGoal\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyGoal").value(30));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .header("X-CSRF-Token", csrfToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issuesASixDigitResetCodeAndAllowsItOnlyOnce() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "reset_auth_1",
                                  "email": "reset_auth_1@example.com",
                                  "password": "old-password"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult forgot = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset_auth_1@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetCode").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                .andReturn();
        String resetCode = extractResetCode(forgot.getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"reset_auth_1@example.com",
                                  "code":"%s",
                                  "newPassword":"new-password"
                                }
                                """.formatted(resetCode)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"reset_auth_1@example.com",
                                  "code":"%s",
                                  "newPassword":"another-password"
                                }
                                """.formatted(resetCode)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reset_auth_1","password":"old-password"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reset_auth_1","password":"new-password"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateUsernameAndEmailAreRejectedCaseInsensitively() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"duplicate_auth_1",
                                  "email":"duplicate_auth_1@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"DUPLICATE_AUTH_1",
                                  "email":"another_auth_1@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"another_auth_1",
                                  "email":"DUPLICATE_AUTH_1@EXAMPLE.COM",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    private MvcResult loginAsAdmin() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Enahaha\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String extractResetCode(String json) {
        Matcher matcher = RESET_CODE_PATTERN.matcher(json);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
