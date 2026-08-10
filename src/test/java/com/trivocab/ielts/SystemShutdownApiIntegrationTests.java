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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trivocab-shutdown-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.auth.allow-demo-user=false"
})
class SystemShutdownApiIntegrationTests {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void shutdownIsRejectedWhenNotEnabledAndForNonAdmins() throws Exception {
        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Enahaha\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession adminSession = session(adminLogin);
        String adminCsrf = csrf(adminSession);

        // app.allow-shutdown defaults to false in tests: even an admin cannot quit.
        mockMvc.perform(post("/api/v1/system/shutdown")
                        .session(adminSession)
                        .header("X-CSRF-Token", adminCsrf))
                .andExpect(status().isForbidden());

        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"shutdown_user",
                                  "displayName":"退出测试",
                                  "email":"shutdown_user@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession userSession = session(registration);
        mockMvc.perform(post("/api/v1/system/shutdown")
                        .session(userSession)
                        .header("X-CSRF-Token", csrf(userSession)))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession session(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrf(MockHttpSession session) {
        return (String) session.getAttribute(AuthSession.CSRF_TOKEN);
    }
}
