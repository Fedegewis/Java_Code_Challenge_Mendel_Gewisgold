package com.challenge.mendel.controller;

import com.challenge.mendel.dto.UpdateTransactionRequest;
import com.challenge.mendel.exception.GlobalExceptionHandler;
import com.challenge.mendel.exception.InvalidParentTransactionException;
import com.challenge.mendel.exception.ParentTransactionNotFoundException;
import com.challenge.mendel.exception.TransactionNotFoundException;
import com.challenge.mendel.exception.ValidationException;
import com.challenge.mendel.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    private TransactionController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(transactionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("PUT /transactions/{transactionId} - Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("should return 200 OK with status ok for valid request")
        void upsertTransaction_ValidRequest_ReturnsOk() throws Exception {
            UpdateTransactionRequest request = new UpdateTransactionRequest(5000.0, "cars", null);
            doNothing().when(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"));

            verify(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));
        }

        @Test
        @DisplayName("should accept request with parent_id")
        void upsertTransaction_WithParent_ReturnsOk() throws Exception {
            UpdateTransactionRequest request = new UpdateTransactionRequest(10000.0, "shopping", 10L);
            doNothing().when(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"));
        }

        @Test
        @DisplayName("should return 200 OK for valid transaction without parent")
        void upsertTransaction_NoParent_ReturnsOk() throws Exception {
            String jsonRequest = "{\"amount\":5000,\"type\":\"cars\"}";
            doNothing().when(transactionService).upsertTransaction(eq(5L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"));

            verify(transactionService).upsertTransaction(eq(5L), any(UpdateTransactionRequest.class));
        }

        @Test
        @DisplayName("should return 200 OK for valid transaction with existing parent")
        void upsertTransaction_WithExistingParent_ReturnsOk() throws Exception {
            String jsonRequest = "{\"amount\":10000,\"type\":\"shopping\",\"parent_id\":1}";
            doNothing().when(transactionService).upsertTransaction(eq(2L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"));

            verify(transactionService).upsertTransaction(eq(2L), any(UpdateTransactionRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT /transactions/{transactionId} - Error Response Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("should return 400 when parent_id does not exist")
        void upsertTransaction_ParentNotFound_ReturnsBadRequest() throws Exception {
            UpdateTransactionRequest request = new UpdateTransactionRequest(5000.0, "cars", 999L);
            doThrow(new ParentTransactionNotFoundException(999L))
                    .when(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parent transaction with id 999 not found"));
        }

        @Test
        @DisplayName("should return 400 when amount is missing")
        void upsertTransaction_MissingAmount_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"type\":\"cars\"}";
            doThrow(new ValidationException("Amount is required"))
                    .when(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Amount is required"));
        }

        @Test
        @DisplayName("should return 400 when type is missing")
        void upsertTransaction_MissingType_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":5000}";
            doThrow(new ValidationException("Type is required"))
                    .when(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Type is required"));
        }

        @Test
        @DisplayName("should return 400 when parent_id in JSON does not exist")
        void upsertTransaction_ParentIdNotFound_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":10000,\"type\":\"shopping\",\"parent_id\":999}";
            doThrow(new ParentTransactionNotFoundException(999L))
                    .when(transactionService).upsertTransaction(eq(11L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/11")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parent transaction with id 999 not found"));

            verify(transactionService).upsertTransaction(eq(11L), any(UpdateTransactionRequest.class));
        }

        @Test
        @DisplayName("should return 400 when amount is negative")
        void upsertTransaction_NegativeAmount_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":-100,\"type\":\"cars\"}";
            doThrow(new ValidationException("Amount must be greater than zero"))
                    .when(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
        }

        @Test
        @DisplayName("should return 400 when amount is zero")
        void upsertTransaction_ZeroAmount_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":0,\"type\":\"cars\"}";
            doThrow(new ValidationException("Amount must be greater than zero"))
                    .when(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
        }

        @Test
        @DisplayName("should return 400 when type is empty string")
        void upsertTransaction_EmptyType_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":100,\"type\":\"\"}";
            doThrow(new ValidationException("Type is required"))
                    .when(transactionService).upsertTransaction(eq(1L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Type is required"));
        }

        @Test
        @DisplayName("should return 400 when parent_id equals transactionId")
        void upsertTransaction_SelfParent_ReturnsBadRequest() throws Exception {
            String jsonRequest = "{\"amount\":100,\"type\":\"cars\",\"parent_id\":5}";
            doThrow(new InvalidParentTransactionException("Transaction cannot be its own parent"))
                    .when(transactionService).upsertTransaction(eq(5L), any(UpdateTransactionRequest.class));

            mockMvc.perform(put("/transactions/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Transaction cannot be its own parent"));

            verify(transactionService).upsertTransaction(eq(5L), any(UpdateTransactionRequest.class));
        }

        @Test
        @DisplayName("should return 400 for malformed JSON")
        void upsertTransaction_MalformedJson_ReturnsBadRequest() throws Exception {
            String malformedJson = "{\"amount\":100,\"type\":";

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for null request body")
        void upsertTransaction_NullBody_ReturnsBadRequest() throws Exception {
            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /transactions/sum/{transactionId} - Happy Path Tests")
    class SumHappyPathTests {

        @Test
        @DisplayName("should return 200 OK with sum for valid transaction")
        void getTransactionSum_ValidId_ReturnsOk() throws Exception {
            when(transactionService.getTransactionSum(10L)).thenReturn(20000.0);

            mockMvc.perform(get("/transactions/sum/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sum").value(20000.0));

            verify(transactionService).getTransactionSum(10L);
        }

        @Test
        @DisplayName("should return 200 OK with sum when transaction has no children")
        void getTransactionSum_NoChildren_ReturnsCorrectSum() throws Exception {
            when(transactionService.getTransactionSum(5L)).thenReturn(5000.0);

            mockMvc.perform(get("/transactions/sum/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sum").value(5000.0));

            verify(transactionService).getTransactionSum(5L);
        }
    }

    @Nested
    @DisplayName("GET /transactions/sum/{transactionId} - Error Response Tests")
    class SumErrorResponseTests {

        @Test
        @DisplayName("should return 404 when transaction does not exist")
        void getTransactionSum_NotFound_ReturnsNotFound() throws Exception {
            when(transactionService.getTransactionSum(999L))
                    .thenThrow(new TransactionNotFoundException(999L));

            mockMvc.perform(get("/transactions/sum/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Transaction with id 999 not found"));

            verify(transactionService).getTransactionSum(999L);
        }
    }
}