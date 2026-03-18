package com.momo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
                "DELETE FROM account_holders",
                "INSERT INTO account_holders (id, full_name, email) VALUES (1, 'Taylor Fox', 'taylor@example.com')",
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (10, 1, 'CHECKING', 100.00)"
        },
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
)
class StatementControllerTest extends ApiTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getStatement() throws Exception {
        createTransaction(Map.of(
                "senderAccountId", 10,
                "amount", 20.00,
                "transactionType", "WITHDRAWAL"
        ));

        createTransaction(Map.of(
                "recipientAccountId", 10,
                "amount", 5.00,
                "transactionType", "DEPOSIT"
        ));

        mockMvc.perform(get("/accounts/{id}/statement", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.currentBalance").value(85.00))
                .andExpect(jsonPath("$.transactions.length()").value(2));
    }

    @Test
    void getStatementByDateRange() throws Exception {
        createTransaction(Map.of(
                "recipientAccountId", 10,
                "amount", 15.00,
                "transactionType", "DEPOSIT"
        ));

        String today = LocalDate.now().toString();

        mockMvc.perform(get("/accounts/{id}/statement", 10)
                        .param("fromDate", today)
                        .param("toDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromDate").value(today))
                .andExpect(jsonPath("$.toDate").value(today))
                .andExpect(jsonPath("$.transactions.length()").value(1));
    }

    private void createTransaction(Map<String, Object> payload) throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }
}
