package com.momo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        statements = {
                "DELETE FROM transactions",
                "DELETE FROM cards",
                "DELETE FROM accounts",
                "DELETE FROM auth_credentials",
                "DELETE FROM account_holders",
                "INSERT INTO account_holders (id, full_name, email) VALUES (1, 'Taylor Fox', 'taylor@example.com')"
        },
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
)
class AuthControllerTest extends ApiTestBase {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void signup() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Alex Doe",
                                "email", "alex@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holderId").exists())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void login() throws Exception {
        insertCredentials();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "taylor@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderId").value(1))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void loginCreatesMissingUser() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "new.user@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderId").exists())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void loginFailsWithBadPassword() throws Exception {
        insertCredentials();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "taylor@example.com",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid login credentials"));
    }

    @Test
    void signupFailsWithDuplicateEmail() throws Exception {
        insertCredentials();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Taylor Fox",
                                "email", "taylor@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    private void insertCredentials() throws Exception {
        String passwordHash = PASSWORD_ENCODER.encode("password123");
        jdbcTemplate.update(
                "INSERT INTO auth_credentials (holder_id, password_hash) VALUES (?, ?)",
                1,
                passwordHash
        );
    }
}
