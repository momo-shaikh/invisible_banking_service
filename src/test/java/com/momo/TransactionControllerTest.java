package com.momo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        statements = {
                "DELETE FROM transactions",
                "DELETE FROM cards",
                "DELETE FROM accounts",
                "DELETE FROM account_holders",
                "INSERT INTO account_holders (id, full_name, email) VALUES (2, 'Casey A', 'casey.a@example.com')",
                "INSERT INTO account_holders (id, full_name, email) VALUES (3, 'Casey B', 'casey.b@example.com')",
                "INSERT INTO account_holders (id, full_name, email) VALUES (4, 'Morgan Low', 'morgan.low@example.com')",
                "INSERT INTO account_holders (id, full_name, email) VALUES (5, 'River One', 'river.one@example.com')",
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (20, 2, 'CHECKING', 100.00)",
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (30, 3, 'SAVINGS', 50.00)",
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (40, 4, 'CHECKING', 10.00)",
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (50, 5, 'CHECKING', 50.00)"
        },
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
)
class TransactionControllerTest extends ApiTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createDeposit() throws Exception {
        long accountA = 20;

        createTransaction(Map.of(
                "recipientAccountId", accountA,
                "amount", 25.00,
                "transactionType", "DEPOSIT"
        ));

        mockMvc.perform(get("/accounts/{id}", accountA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.00));

        mockMvc.perform(get("/accounts/{id}/transactions", accountA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createWithdrawal() throws Exception {
        long accountA = 20;

        createTransaction(Map.of(
                "senderAccountId", accountA,
                "amount", 40.00,
                "transactionType", "WITHDRAWAL"
        ));

        mockMvc.perform(get("/accounts/{id}", accountA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(60.00));

        mockMvc.perform(get("/accounts/{id}/transactions", accountA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createTransfer() throws Exception {
        long accountA = 20;
        long accountB = 30;

        createTransaction(Map.of(
                "senderAccountId", accountA,
                "recipientAccountId", accountB,
                "amount", 20.00,
                "transactionType", "TRANSFER"
        ));

        mockMvc.perform(get("/accounts/{id}", accountA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(80.00));

        mockMvc.perform(get("/accounts/{id}", accountB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));

        mockMvc.perform(get("/accounts/{id}/transactions", accountA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void withdrawalFailsWhenInsufficientFunds() throws Exception {
        long account = 40;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "senderAccountId", account,
                                "amount", 25.00,
                                "transactionType", "WITHDRAWAL"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient funds"));
    }

    @Test
    void transferFailsWhenRecipientMissing() throws Exception {
        long account = 50;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "senderAccountId", account,
                                "recipientAccountId", 999999,
                                "amount", 5.00,
                                "transactionType", "TRANSFER"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sender or recipient account not found"));
    }

    @Test
    void transferFailsWhenSenderMissing() throws Exception {
        long account = 50;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "senderAccountId", 999999,
                                "recipientAccountId", account,
                                "amount", 5.00,
                                "transactionType", "TRANSFER"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sender or recipient account not found"));
    }

    private void createTransaction(Map<String, Object> payload) throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }
}
