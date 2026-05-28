package com.challenge.mendel.service;

import com.challenge.mendel.domain.Transaction;
import com.challenge.mendel.dto.UpdateTransactionRequest;
import com.challenge.mendel.exception.ParentTransactionNotFoundException;
import com.challenge.mendel.exception.ValidationException;
import com.challenge.mendel.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository repository;

    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(repository);
    }

    @Nested
    @DisplayName("upsertTransaction() - Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("should save transaction successfully with all fields")
        void upsertTransaction_AllFields_Success() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(5000.0, "cars", 10L);
            when(repository.existsById(10L)).thenReturn(true);

            service.upsertTransaction(1L, request);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(repository).save(captor.capture());

            Transaction saved = captor.getValue();
            assertEquals(1L, saved.getId());
            assertEquals(5000.0, saved.getAmount());
            assertEquals("cars", saved.getType());
            assertEquals(10L, saved.getParentId());
        }

        @Test
        @DisplayName("should save transaction without parent_id")
        void upsertTransaction_NoParent_Success() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(10000.0, "shopping", null);

            service.upsertTransaction(1L, request);

            verify(repository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should save transaction when parent exists")
        void upsertTransaction_ParentExists_Success() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(500.0, "debit", 10L);
            when(repository.existsById(10L)).thenReturn(true);

            service.upsertTransaction(1L, request);

            verify(repository).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("upsertTransaction() - Validation Error Tests")
    class ValidationErrorTests {

        @Test
        @DisplayName("should throw exception when amount is null")
        void upsertTransaction_NullAmount_ThrowsValidationException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(null, "cars", null);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> service.upsertTransaction(1L, request));

            assertEquals("Amount is required", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when type is null")
        void upsertTransaction_NullType_ThrowsValidationException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, null, null);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> service.upsertTransaction(1L, request));

            assertEquals("Type is required", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when type is blank")
        void upsertTransaction_BlankType_ThrowsValidationException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, "  ", null);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> service.upsertTransaction(1L, request));

            assertEquals("Type is required", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when amount is negative")
        void upsertTransaction_NegativeAmount_ThrowsValidationException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(-100.0, "cars", null);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> service.upsertTransaction(1L, request));

            assertEquals("Amount must be greater than zero", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when amount is zero")
        void upsertTransaction_ZeroAmount_ThrowsValidationException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(0.0, "cars", null);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> service.upsertTransaction(1L, request));

            assertEquals("Amount must be greater than zero", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when parent_id equals transactionId (self-parent)")
        void upsertTransaction_SelfParent_ThrowsValidationException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, "cars", 5L);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> service.upsertTransaction(5L, request));

            assertEquals("Transaction cannot be its own parent", exception.getMessage());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("upsertTransaction() - Parent Validation Tests")
    class ParentValidationTests {

        @Test
        @DisplayName("should throw exception when parent_id does not exist")
        void upsertTransaction_ParentNotExists_ThrowsParentNotFoundException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(5000.0, "cars", 999L);
            when(repository.existsById(999L)).thenReturn(false);

            ParentTransactionNotFoundException exception = assertThrows(
                    ParentTransactionNotFoundException.class,
                    () -> service.upsertTransaction(1L, request));

            assertEquals("Parent transaction with id 999 not found", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should not check parent existence when parent_id is null")
        void upsertTransaction_NullParent_DoesNotCheckParent() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, "debit", null);

            service.upsertTransaction(1L, request);

            verify(repository, never()).existsById(any());
            verify(repository).save(any(Transaction.class));
        }
    }
}