package com.renatoganske.gestao_de_eventos.configs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof, through the real HTTP layer, that repeated failed logins get
 * locked out (ADR-0017 follow-up: no rate limiting was the gap the security review
 * flagged on PR #29 for GDE-30).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginRateLimitIntegrationTest {

    private static final String USERNAME = "lockout-test-user";
    private static final String CORRECT_PASSWORD = "correct-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void login_afterFiveFailedAttempts_blocksEvenACorrectPasswordWith429() throws Exception {
        UserDetails userDetails = User.withUsername(USERNAME)
                .password(passwordEncoder.encode(CORRECT_PASSWORD))
                .authorities("USER")
                .build();
        when(appUserDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);

        for (int i = 0; i < 5; i++) {
            attemptLogin(CORRECT_PASSWORD + "-wrong")
                    .andExpect(status().isUnauthorized());
        }

        attemptLogin(CORRECT_PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    private org.springframework.test.web.servlet.ResultActions attemptLogin(String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginAttempt(USERNAME, password));
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private record LoginAttempt(String username, String password) {
    }
}
