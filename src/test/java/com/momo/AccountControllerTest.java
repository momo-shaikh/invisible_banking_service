package com.momo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.MvcResult;

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
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (10, 1, 'CHECKING', 25.00)"
        },
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
)
class AccountControllerTest extends ApiTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAccount() throws Exception {
        MvcResult created = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "holderId", 1,
                                "accountType", "CHECKING",
                                "balance", 25.00
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.holderId").value(1))
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.balance").value(25.00))
                .andReturn();

        long accountId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId));
    }

    @Test
    void deleteFundedAccount() throws Exception {
        mockMvc.perform(delete("/accounts/{id}", 10))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/{id}", 10))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account not found"));
    }

    @Test
    void deleteAccountAlsoDeletesCardsAndTransactions() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "holderId", 1,
                                "accountType", "CHECKING",
                                "balance", 25.00
                        ))))
                .andExpect(status().isCreated())
                .andDo(result -> {
                    long accountId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

                    mockMvc.perform(post("/accounts/{id}/cards", accountId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(Map.of(
                                            "type", "DEBIT",
                                            "status", "ACTIVE"
                                    ))))
                            .andExpect(status().isCreated());

                    mockMvc.perform(post("/transactions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(Map.of(
                                            "recipientAccountId", accountId,
                                            "amount", 5.00,
                                            "transactionType", "DEPOSIT"
                                    ))))
                            .andExpect(status().isCreated());

                    mockMvc.perform(delete("/accounts/{id}", accountId))
                            .andExpect(status().isNoContent());

                    mockMvc.perform(get("/accounts/{id}", accountId))
                            .andExpect(status().isNotFound())
                            .andExpect(jsonPath("$.error").value("Account not found"));

                    mockMvc.perform(get("/accounts/{id}/cards/status", accountId))
                            .andExpect(status().isNotFound())
                            .andExpect(jsonPath("$.error").value("Account not found"));

                    mockMvc.perform(get("/accounts/{id}/statement", accountId))
                            .andExpect(status().isNotFound())
                            .andExpect(jsonPath("$.error").value("Account not found"));
                });
    }

}
