package com.trivocab.ielts;

import com.trivocab.ielts.common.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-mode-api-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.auth.allow-demo-user=false"
})
class LearningModeApiIntegrationTests {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void defaultsToSimpleThenSwitchesToImmersive() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"mode_user",
                                  "displayName":"模式用户",
                                  "email":"mode_user@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        String csrf = (String) session.getAttribute(AuthSession.CSRF_TOKEN);

        mockMvc.perform(get("/api/v1/profile/learning-mode").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningMode").value("SIMPLE"));

        mockMvc.perform(put("/api/v1/profile/learning-mode")
                        .session(session)
                        .header("X-CSRF-Token", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learningMode\":\"IMMERSIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningMode").value("IMMERSIVE"));

        mockMvc.perform(get("/api/v1/profile/learning-mode").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningMode").value("IMMERSIVE"));

        mockMvc.perform(put("/api/v1/profile/learning-mode")
                        .session(session)
                        .header("X-CSRF-Token", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learningMode\":\"SIMPLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningMode").value("SIMPLE"));
    }

    @Test
    void rejectsUnknownLearningMode() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"mode_user_bad",
                                  "displayName":"模式坏用户",
                                  "email":"mode_user_bad@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        String csrf = (String) session.getAttribute(AuthSession.CSRF_TOKEN);

        mockMvc.perform(put("/api/v1/profile/learning-mode")
                        .session(session)
                        .header("X-CSRF-Token", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learningMode\":\"WEIRD\"}"))
                .andExpect(status().isBadRequest());
    }
}
