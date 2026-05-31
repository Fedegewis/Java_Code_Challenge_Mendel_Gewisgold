package com.challenge.mendel.controller;

import com.challenge.mendel.dto.UpdateTransactionRequest;
import com.challenge.mendel.repository.InMemoryTransactionRepository;
import com.challenge.mendel.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private InMemoryTransactionRepository transactionRepository;

    @Nested
    @DisplayName("PUT /transactions/{transactionId} - Integration Tests")
    class IntegrationTests {

        @BeforeEach
        void setUp() {
            transactionRepository.findAll().forEach(t -> transactionRepository.save(t));
        }

        @Test
        void upsertTransaction_ValidRequest_ReturnsOk() throws Exception {
            String jsonRequest = "{\"amount\":5000,\"type\":\"cars\"}";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"));
        }

        @Test
        void upsertTransaction_WithExistingParent_ReturnsOk() throws Exception {
            transactionService.upsertTransaction(1L,
                    new UpdateTransactionRequest(1000.0, "credit", null));

            String jsonRequest = "{\"amount\":5000,\"type\":\"shopping\",\"parent_id\":1}";

            mockMvc.perform(put("/transactions/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"));
        }

        @Test
        void upsertTransaction_NonExistingParent_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":5000,\"type\":\"cars\",\"parent_id\":999}";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parent transaction with id 999 not found"));
        }

        @Test
        void upsertTransaction_NegativeAmount_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":-100,\"type\":\"cars\"}";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
        }

        @Test
        void upsertTransaction_ZeroAmount_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":0,\"type\":\"cars\"}";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
        }

        @Test
        void upsertTransaction_MissingAmount_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"type\":\"cars\"}";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Amount is required"));
        }

        @Test
        void upsertTransaction_MissingType_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":5000}";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Type is required"));
        }

        @Test
        void upsertTransaction_BlankType_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":100,\"type\":\"\"}";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Type is required"));
        }

        @Test
        void upsertTransaction_SelfParent_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":100,\"type\":\"cars\",\"parent_id\":5}";

            mockMvc.perform(put("/transactions/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Transaction cannot be its own parent"));
        }

        @Test
        void upsertTransaction_MalformedJson_ReturnsBadRequest() throws Exception {
            String malformedJson = "{\"amount\":100,\"type\":";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }
    }
}