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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:build/demo-seed-test.db",
        "app.demo-seed.enabled=true"
})
class DemoDataSeederTest extends ApiTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seededDemoUserCanLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "ada@example.com",
                                "password", "demo123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderId").exists())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void seededDemoUserHasAccounts() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "ada@example.com",
                                "password", "demo123"
                        ))))
                .andExpect(status().isOk())
                .andDo(result -> {
                    long holderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("holderId").asLong();

                    mockMvc.perform(get("/holders/{id}/accounts", holderId))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.length()").value(2));
                });
    }
}
