package com.challenge.mendel.service;

import com.challenge.mendel.domain.Transaction;
import com.challenge.mendel.dto.UpdateTransactionRequest;
import com.challenge.mendel.exception.InvalidParentTransactionException;
import com.challenge.mendel.exception.ParentTransactionNotFoundException;
import com.challenge.mendel.exception.TransactionNotFoundException;
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

import java.util.List;

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
        void upsertTransaction_SelfParent_ThrowsInvalidParentException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, "cars", 5L);

            InvalidParentTransactionException exception = assertThrows(InvalidParentTransactionException.class,
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

        @Test
        @DisplayName("should throw exception when parent_id would create indirect cycle")
        void upsertTransaction_IndirectCycle_ThrowsInvalidParentException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, "cars", 2L);

            when(repository.findById(2L)).thenReturn(new Transaction(2L, 100.0, "cars", 1L));

            InvalidParentTransactionException exception = assertThrows(
                    InvalidParentTransactionException.class,
                    () -> service.upsertTransaction(1L, request));

            assertEquals("Setting this parent would create a cycle", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when parent_id would create deep cycle")
        void upsertTransaction_DeepCycle_ThrowsInvalidParentException() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, "cars", 3L);

            when(repository.findById(3L)).thenReturn(new Transaction(3L, 100.0, "cars", 2L));
            when(repository.findById(2L)).thenReturn(new Transaction(2L, 100.0, "cars", 1L));

            InvalidParentTransactionException exception = assertThrows(
                    InvalidParentTransactionException.class,
                    () -> service.upsertTransaction(1L, request));

            assertEquals("Setting this parent would create a cycle", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should allow valid parent chain without cycle")
        void upsertTransaction_ValidParentChain_Success() {
            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, "cars", 5L);

            when(repository.findById(5L)).thenReturn(new Transaction(5L, 100.0, "cars", 4L));
            when(repository.existsById(5L)).thenReturn(true);

            service.upsertTransaction(1L, request);

            verify(repository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should not modify repository when cycle is detected")
        void upsertTransaction_CycleDetected_DoesNotModifyRepository() {
            when(repository.findById(2L)).thenReturn(new Transaction(2L, 100.0, "cars", 1L));

            UpdateTransactionRequest request = new UpdateTransactionRequest(100.0, "cars", 2L);

            assertThrows(InvalidParentTransactionException.class,
                    () -> service.upsertTransaction(1L, request));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTransactionIdsByType() - Happy Path Tests")
    class GetTransactionIdsByTypeHappyPathTests {

        @Test
        @DisplayName("should return transaction ids matching the given type")
        void getTransactionIdsByType_ExistingType_ReturnsIds() {
            Transaction t1 = new Transaction(10L, 100.0, "cars", null);
            Transaction t2 = new Transaction(12L, 200.0, "cars", null);
            when(repository.findByType("cars")).thenReturn(List.of(t1, t2));

            List<Long> result = service.getTransactionIdsByType("cars");

            assertEquals(List.of(10L, 12L), result);
        }

        @Test
        @DisplayName("should return empty list when no transactions match type")
        void getTransactionIdsByType_NoMatch_ReturnsEmptyList() {
            when(repository.findByType("unknown")).thenReturn(List.of());

            List<Long> result = service.getTransactionIdsByType("unknown");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return single id when only one transaction matches type")
        void getTransactionIdsByType_SingleMatch_ReturnsSingleId() {
            Transaction t1 = new Transaction(5L, 100.0, "shopping", null);
            when(repository.findByType("shopping")).thenReturn(List.of(t1));

            List<Long> result = service.getTransactionIdsByType("shopping");

            assertEquals(List.of(5L), result);
        }
    }

    @Nested
    @DisplayName("getTransactionIdsByType() - Edge Case Tests")
    class GetTransactionIdsByTypeEdgeCaseTests {

        @Test
        @DisplayName("should return empty list for null type")
        void getTransactionIdsByType_NullType_ReturnsEmptyList() {
            List<Long> result = service.getTransactionIdsByType(null);

            assertTrue(result.isEmpty());
            verify(repository, never()).findByType(any());
        }

        @Test
        @DisplayName("should return empty list for blank type")
        void getTransactionIdsByType_BlankType_ReturnsEmptyList() {
            List<Long> result = service.getTransactionIdsByType("   ");

            assertTrue(result.isEmpty());
            verify(repository, never()).findByType(any());
        }

        @Test
        @DisplayName("should return empty list when repository returns null")
        void getTransactionIdsByType_NullFromRepository_ReturnsEmptyList() {
            when(repository.findByType("test")).thenReturn(null);

            List<Long> result = service.getTransactionIdsByType("test");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getChildrenIdsByParentId() - Happy Path Tests")
    class GetChildrenIdsByParentIdHappyPathTests {

        @Test
        @DisplayName("should return child ids when parent has children")
        void getChildrenIdsByParentId_HasChildren_ReturnsChildIds() {
            when(repository.findChildrenIdsByParentId(10L)).thenReturn(List.of(11L, 12L));

            List<Long> result = service.getChildrenIdsByParentId(10L);

            assertEquals(List.of(11L, 12L), result);
        }

        @Test
        @DisplayName("should return empty list when parent has no children")
        void getChildrenIdsByParentId_NoChildren_ReturnsEmptyList() {
            when(repository.findChildrenIdsByParentId(10L)).thenReturn(List.of());

            List<Long> result = service.getChildrenIdsByParentId(10L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return single child when parent has one child")
        void getChildrenIdsByParentId_SingleChild_ReturnsSingleId() {
            when(repository.findChildrenIdsByParentId(10L)).thenReturn(List.of(11L));

            List<Long> result = service.getChildrenIdsByParentId(10L);

            assertEquals(List.of(11L), result);
        }
    }

    @Nested
    @DisplayName("getChildrenIdsByParentId() - Edge Case Tests")
    class GetChildrenIdsByParentIdEdgeCaseTests {

        @Test
        @DisplayName("should return empty list for null parent id")
        void getChildrenIdsByParentId_NullParentId_ReturnsEmptyList() {
            List<Long> result = service.getChildrenIdsByParentId(null);

            assertTrue(result.isEmpty());
            verify(repository, never()).findChildrenIdsByParentId(any());
        }

        @Test
        @DisplayName("should return empty list when parent does not exist")
        void getChildrenIdsByParentId_ParentNotExists_ReturnsEmptyList() {
            when(repository.findChildrenIdsByParentId(999L)).thenReturn(List.of());

            List<Long> result = service.getChildrenIdsByParentId(999L);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getTransactionSum() - Happy Path Tests")
    class GetTransactionSumHappyPathTests {

        @Test
        @DisplayName("should return transaction amount when transaction has no children")
        void getTransactionSum_NoChildren_ReturnsOnlyAmount() {
            Transaction transaction = new Transaction(10L, 5000.0, "cars", null);
            when(repository.findById(10L)).thenReturn(transaction);
            when(repository.findChildrenIdsByParentId(10L)).thenReturn(List.of());

            Double sum = service.getTransactionSum(10L);

            assertEquals(5000.0, sum);
        }

        @Test
        @DisplayName("should return sum of transaction and direct children")
        void getTransactionSum_DirectChildren_ReturnsSumWithChildren() {
            Transaction parent = new Transaction(10L, 5000.0, "cars", null);
            Transaction child1 = new Transaction(11L, 10000.0, "shopping", 10L);
            Transaction child2 = new Transaction(12L, 5000.0, "shopping", 10L);

            when(repository.findById(10L)).thenReturn(parent);
            when(repository.findById(11L)).thenReturn(child1);
            when(repository.findById(12L)).thenReturn(child2);
            when(repository.findChildrenIdsByParentId(10L)).thenReturn(List.of(11L, 12L));
            when(repository.findChildrenIdsByParentId(11L)).thenReturn(List.of());
            when(repository.findChildrenIdsByParentId(12L)).thenReturn(List.of());

            Double sum = service.getTransactionSum(10L);

            assertEquals(20000.0, sum);
        }

        @Test
        @DisplayName("should return sum with transitive descendants (grandchildren)")
        void getTransactionSum_Grandchildren_ReturnsTransitiveSum() {
            Transaction grandparent = new Transaction(10L, 5000.0, "cars", null);
            Transaction parent = new Transaction(11L, 10000.0, "shopping", 10L);
            Transaction child = new Transaction(12L, 5000.0, "shopping", 11L);

            when(repository.findById(10L)).thenReturn(grandparent);
            when(repository.findById(11L)).thenReturn(parent);
            when(repository.findById(12L)).thenReturn(child);
            when(repository.findChildrenIdsByParentId(10L)).thenReturn(List.of(11L));
            when(repository.findChildrenIdsByParentId(11L)).thenReturn(List.of(12L));
            when(repository.findChildrenIdsByParentId(12L)).thenReturn(List.of());

            Double sum = service.getTransactionSum(10L);

            assertEquals(20000.0, sum);
        }
    }

    @Nested
    @DisplayName("getTransactionSum() - Error Tests")
    class GetTransactionSumErrorTests {

        @Test
        @DisplayName("should throw TransactionNotFoundException when transaction does not exist")
        void getTransactionSum_NotFound_ThrowsException() {
            when(repository.findById(999L)).thenReturn(null);

            TransactionNotFoundException exception = assertThrows(
                    TransactionNotFoundException.class,
                    () -> service.getTransactionSum(999L));

            assertEquals("Transaction with id 999 not found", exception.getMessage());
        }

        @Test
        @DisplayName("should throw ValidationException when transactionId is null")
        void getTransactionSum_NullId_ThrowsValidationException() {
            ValidationException exception = assertThrows(
                    ValidationException.class,
                    () -> service.getTransactionSum(null));

            assertEquals("Transaction ID is required", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getTransactionSum() - Edge Case Tests")
    class GetTransactionSumEdgeCaseTests {

        @Test
        @DisplayName("should handle single child correctly")
        void getTransactionSum_SingleChild_ReturnsCorrectSum() {
            Transaction parent = new Transaction(10L, 5000.0, "cars", null);
            Transaction child = new Transaction(11L, 3000.0, "debit", 10L);

            when(repository.findById(10L)).thenReturn(parent);
            when(repository.findById(11L)).thenReturn(child);
            when(repository.findChildrenIdsByParentId(10L)).thenReturn(List.of(11L));
            when(repository.findChildrenIdsByParentId(11L)).thenReturn(List.of());

            Double sum = service.getTransactionSum(10L);

            assertEquals(8000.0, sum);
        }

        @Test
        @DisplayName("should return only amount when asking sum of leaf child")
        void getTransactionSum_LeafNode_ReturnsOnlyAmount() {
            Transaction child = new Transaction(11L, 5000.0, "shopping", 10L);

            when(repository.findById(11L)).thenReturn(child);
            when(repository.findChildrenIdsByParentId(11L)).thenReturn(List.of());

            Double sum = service.getTransactionSum(11L);

            assertEquals(5000.0, sum);
        }

        @Test
        @DisplayName("should handle deep hierarchy chain correctly")
        void getTransactionSum_DeepHierarchy_ReturnsCorrectSum() {
            Transaction t10 = new Transaction(10L, 1000.0, "root", null);
            Transaction t11 = new Transaction(11L, 1000.0, "level1", 10L);
            Transaction t12 = new Transaction(12L, 1000.0, "level2", 11L);
            Transaction t13 = new Transaction(13L, 1000.0, "level3", 12L);

            when(repository.findById(10L)).thenReturn(t10);
            when(repository.findById(11L)).thenReturn(t11);
            when(repository.findById(12L)).thenReturn(t12);
            when(repository.findById(13L)).thenReturn(t13);
            when(repository.findChildrenIdsByParentId(10L)).thenReturn(List.of(11L));
            when(repository.findChildrenIdsByParentId(11L)).thenReturn(List.of(12L));
            when(repository.findChildrenIdsByParentId(12L)).thenReturn(List.of(13L));
            when(repository.findChildrenIdsByParentId(13L)).thenReturn(List.of());

            Double sum = service.getTransactionSum(10L);

            assertEquals(4000.0, sum);
        }
    }
}