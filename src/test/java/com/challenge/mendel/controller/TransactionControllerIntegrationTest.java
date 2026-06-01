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
            transactionRepository.clear();
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
        void upsertTransaction_IndirectCycle_ReturnsBadRequest() throws Exception {
            transactionService.upsertTransaction(1L, new UpdateTransactionRequest(100.0, "cars", null));
            transactionService.upsertTransaction(2L, new UpdateTransactionRequest(200.0, "cars", 1L));

            String jsonRequest = "{\"amount\":300,\"type\":\"cars\",\"parent_id\":2}";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Setting this parent would create a cycle"));
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

    @Nested
    @DisplayName("GET /transactions/types/{type} - Integration Tests")
    class GetTransactionsByTypeIntegrationTests {

        @BeforeEach
        void setUp() {
            transactionRepository.clear();
        }

        @Test
        void getTransactionsByType_ReturnsCorrectIds() throws Exception {
            transactionService.upsertTransaction(1L, new UpdateTransactionRequest(100.0, "cars", null));
            transactionService.upsertTransaction(2L, new UpdateTransactionRequest(200.0, "cars", null));
            transactionService.upsertTransaction(3L, new UpdateTransactionRequest(300.0, "shopping", null));

            mockMvc.perform(get("/transactions/types/cars"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value(1))
                    .andExpect(jsonPath("$[1]").value(2));
        }

        @Test
        void getTransactionsByType_NoMatch_ReturnsEmptyArray() throws Exception {
            transactionService.upsertTransaction(1L, new UpdateTransactionRequest(100.0, "cars", null));

            mockMvc.perform(get("/transactions/types/unknown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void getTransactionsByType_BlankType_ReturnsEmptyArray() throws Exception {
            transactionService.upsertTransaction(1L, new UpdateTransactionRequest(100.0, "cars", null));

            mockMvc.perform(get("/transactions/types/%20%20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /transactions/sum/{transactionId} - Integration Tests")
    class GetTransactionSumIntegrationTests {

        @BeforeEach
        void setUp() {
            transactionRepository.clear();
        }

        @Test
        void getTransactionSum_ValidTransaction_ReturnsCorrectSum() throws Exception {
            transactionService.upsertTransaction(1L, new UpdateTransactionRequest(5000.0, "cars", null));

            mockMvc.perform(get("/transactions/sum/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sum").value(5000.0));
        }

        @Test
        void getTransactionSum_WithTransitiveChildren_ReturnsCorrectSum() throws Exception {
            transactionService.upsertTransaction(1L, new UpdateTransactionRequest(5000.0, "cars", null));
            transactionService.upsertTransaction(2L, new UpdateTransactionRequest(3000.0, "debit", 1L));
            transactionService.upsertTransaction(3L, new UpdateTransactionRequest(2000.0, "debit", 2L));

            mockMvc.perform(get("/transactions/sum/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sum").value(10000.0));
        }

        @Test
        void getTransactionSum_MissingTransaction_ReturnsNotFound() throws Exception {
            mockMvc.perform(get("/transactions/sum/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Transaction with id 999 not found"));
        }

        @Test
        void getTransactionSum_TransactionWithNoChildren_ReturnsOnlyAmount() throws Exception {
            transactionService.upsertTransaction(10L, new UpdateTransactionRequest(7500.0, "shopping", null));

            mockMvc.perform(get("/transactions/sum/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sum").value(7500.0));
        }
    }
}