package com.momo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                "INSERT INTO account_holders (id, full_name, email) VALUES (1, 'Taylor Fox', 'taylor@example.com')",
                "INSERT INTO account_holders (id, full_name, email) VALUES (2, 'Credit User', 'credit@example.com')",
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (10, 1, 'CHECKING', 25.00)",
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (20, 2, 'CREDIT', 0.00)"
        },
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
)
class CardControllerTest extends ApiTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createDebitCard() throws Exception {
        mockMvc.perform(post("/accounts/{id}/cards", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "DEBIT",
                                "status", "ACTIVE"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.cardLimit").value(25.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectCreditCardForCheckingAccount() throws Exception {
        mockMvc.perform(post("/accounts/{id}/cards", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "CREDIT",
                                "status", "ACTIVE"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Card type does not match the account type"));
    }

    @Test
    void rejectDebitCardForCreditAccount() throws Exception {
        mockMvc.perform(post("/accounts/{id}/cards", 20)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "DEBIT",
                                "status", "ACTIVE"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Card type does not match the account type"));
    }

    @Test
    void getCardStatus() throws Exception {
        createCard(10, "DEBIT", "ACTIVE");

        mockMvc.perform(get("/accounts/{id}/cards/status", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getCardLimit() throws Exception {
        createCard(20, "CREDIT", "ACTIVE");

        mockMvc.perform(get("/accounts/{id}/cards/limit", 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardLimit").value(10000));
    }

    @Test
    void updateCardStatus() throws Exception {
        createCard(10, "DEBIT", "ACTIVE");

        mockMvc.perform(patch("/accounts/{id}/cards/status", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "FROZEN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        mockMvc.perform(patch("/accounts/{id}/cards/status", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private void createCard(long accountId, String type, String status) throws Exception {
        mockMvc.perform(post("/accounts/{id}/cards", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", type,
                                "status", status
                        ))))
                .andExpect(status().isCreated());
    }
}
