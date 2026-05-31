package com.challenge.mendel.repository;

import com.challenge.mendel.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTransactionRepositoryTest {

    private InMemoryTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
    }

    @Nested
    @DisplayName("save() tests")
    class SaveTests {

        @Test
        @DisplayName("should save transaction and make it retrievable by id")
        void saveTransaction_MakesItFindable() {
            Transaction transaction = new Transaction(1L, 100.0, "CREDIT", null);

            repository.save(transaction);

            Transaction found = repository.findById(1L);
            assertNotNull(found);
            assertEquals(1L, found.getId());
            assertEquals(100.0, found.getAmount());
            assertEquals("CREDIT", found.getType());
            assertNull(found.getParentId());
        }

        @Test
        @DisplayName("should overwrite existing transaction with same id")
        void saveTransaction_OverwritesExisting() {
            Transaction original = new Transaction(1L, 100.0, "CREDIT", null);
            Transaction updated = new Transaction(1L, 200.0, "DEBIT", 1L);

            repository.save(original);
            repository.save(updated);

            Transaction found = repository.findById(1L);
            assertEquals(200.0, found.getAmount());
            assertEquals("DEBIT", found.getType());
            assertEquals(1L, found.getParentId());
        }

        @Test
        @DisplayName("should save multiple transactions with different ids")
        void saveMultipleTransactions_AllFindable() {
            Transaction t1 = new Transaction(1L, 100.0, "CREDIT", null);
            Transaction t2 = new Transaction(2L, 200.0, "DEBIT", null);
            Transaction t3 = new Transaction(3L, 300.0, "CREDIT", 1L);

            repository.save(t1);
            repository.save(t2);
            repository.save(t3);

            assertNotNull(repository.findById(1L));
            assertNotNull(repository.findById(2L));
            assertNotNull(repository.findById(3L));
        }
    }

    @Nested
    @DisplayName("findById() tests")
    class FindByIdTests {

        @Test
        @DisplayName("should return transaction when it exists")
        void findById_ExistingId_ReturnsTransaction() {
            Transaction transaction = new Transaction(1L, 150.0, "CREDIT", null);
            repository.save(transaction);

            Transaction found = repository.findById(1L);

            assertNotNull(found);
            assertEquals(1L, found.getId());
            assertEquals(150.0, found.getAmount());
        }

        @Test
        @DisplayName("should return null when transaction does not exist")
        void findById_NonExistingId_ReturnsNull() {
            Transaction found = repository.findById(999L);

            assertNull(found);
        }

        @Test
        @DisplayName("should return null for null id")
        void findById_NullId_ReturnsNull() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            Transaction found = repository.findById(null);

            assertNull(found);
        }
    }

    @Nested
    @DisplayName("findAll() tests")
    class FindAllTests {

        @Test
        @DisplayName("should return empty list when no transactions saved")
        void findAll_NoTransactions_ReturnsEmptyList() {
            List<Transaction> result = repository.findAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return single transaction when one saved")
        void findAll_OneTransaction_ReturnsListWithOne() {
            Transaction transaction = new Transaction(1L, 100.0, "CREDIT", null);
            repository.save(transaction);

            List<Transaction> result = repository.findAll();

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
        }

        @Test
        @DisplayName("should return all saved transactions")
        void findAll_MultipleTransactions_ReturnsAll() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));
            repository.save(new Transaction(2L, 200.0, "DEBIT", null));
            repository.save(new Transaction(3L, 300.0, "CREDIT", 1L));

            List<Transaction> result = repository.findAll();

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("should return new list instance (not modifying original)")
        void findAll_ReturnsNewListInstance() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            List<Transaction> result1 = repository.findAll();
            List<Transaction> result2 = repository.findAll();

            assertNotSame(result1, result2);
            assertEquals(result1.size(), result2.size());
        }
    }

    

    @Nested
    @DisplayName("existsById() tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("should return true when transaction exists")
        void existsById_ExistingId_ReturnsTrue() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            boolean exists = repository.existsById(1L);

            assertTrue(exists);
        }

        @Test
        @DisplayName("should return false when transaction does not exist")
        void existsById_NonExistingId_ReturnsFalse() {
            boolean exists = repository.existsById(999L);

            assertFalse(exists);
        }

        @Test
        @DisplayName("should return false for null id")
        void existsById_NullId_ReturnsFalse() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            boolean exists = repository.existsById(null);

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("findByType() tests")
    class FindByTypeTests {

        @Test
        @DisplayName("should return empty list when no transactions of type exist")
        void findByType_NoTransactionsOfType_ReturnsEmptyList() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            List<Transaction> result = repository.findByType("DEBIT");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return transactions matching the given type")
        void findByType_ExistingType_ReturnsMatchingTransactions() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));
            repository.save(new Transaction(2L, 200.0, "DEBIT", null));
            repository.save(new Transaction(3L, 300.0, "CREDIT", 1L));

            List<Transaction> result = repository.findByType("CREDIT");

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should return empty list when repository is empty")
        void findByType_EmptyRepository_ReturnsEmptyList() {
            List<Transaction> result = repository.findByType("CREDIT");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list for null type")
        void findByType_NullType_ReturnsEmptyList() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            List<Transaction> result = repository.findByType(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list for blank type")
        void findByType_BlankType_ReturnsEmptyList() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            List<Transaction> result = repository.findByType("   ");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not return modified original list when updating")
        void findByType_ReturnsNewListInstance() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            List<Transaction> result1 = repository.findByType("CREDIT");
            List<Transaction> result2 = repository.findByType("CREDIT");

            assertNotSame(result1, result2);
        }

        @Test
        @DisplayName("should update index when transaction type changes")
        void findByType_TransactionTypeChanged_ReturnsUpdatedList() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            repository.save(new Transaction(1L, 100.0, "DEBIT", null));

            List<Transaction> creditResult = repository.findByType("CREDIT");
            List<Transaction> debitResult = repository.findByType("DEBIT");

            assertTrue(creditResult.isEmpty());
            assertEquals(1, debitResult.size());
        }

        @Test
        @DisplayName("should remove transaction from type index when deleted (overwritten)")
        void findByType_TransactionOverwritten_RemovedFromOldTypeIndex() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));
            repository.save(new Transaction(2L, 200.0, "DEBIT", null));

            repository.save(new Transaction(1L, 150.0, "DEBIT", null));

            List<Transaction> creditResult = repository.findByType("CREDIT");
            List<Transaction> debitResult = repository.findByType("DEBIT");

            assertTrue(creditResult.isEmpty());
            assertEquals(2, debitResult.size());
        }
    }

    @Nested
    @DisplayName("findChildrenIdsByParentId() tests")
    class FindChildrenIdsByParentIdTests {

        @Test
        @DisplayName("should return empty list when parent has no children")
        void findChildrenIdsByParentId_NoChildren_ReturnsEmptyList() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            List<Long> result = repository.findChildrenIdsByParentId(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return child ids when parent has children")
        void findChildrenIdsByParentId_HasChildren_ReturnsChildIds() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));
            repository.save(new Transaction(2L, 200.0, "DEBIT", 1L));
            repository.save(new Transaction(3L, 300.0, "CREDIT", 1L));

            List<Long> result = repository.findChildrenIdsByParentId(1L);

            assertEquals(2, result.size());
            assertTrue(result.contains(2L));
            assertTrue(result.contains(3L));
        }

        @Test
        @DisplayName("should return empty list when parent does not exist")
        void findChildrenIdsByParentId_ParentNotExists_ReturnsEmptyList() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            List<Long> result = repository.findChildrenIdsByParentId(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list for null parent id")
        void findChildrenIdsByParentId_NullParentId_ReturnsEmptyList() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));

            List<Long> result = repository.findChildrenIdsByParentId(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list when repository is empty")
        void findChildrenIdsByParentId_EmptyRepository_ReturnsEmptyList() {
            List<Long> result = repository.findChildrenIdsByParentId(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return new list instance (not modifying original)")
        void findChildrenIdsByParentId_ReturnsNewListInstance() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));
            repository.save(new Transaction(2L, 200.0, "DEBIT", 1L));

            List<Long> result1 = repository.findChildrenIdsByParentId(1L);
            List<Long> result2 = repository.findChildrenIdsByParentId(1L);

            assertNotSame(result1, result2);
        }

        @Test
        @DisplayName("should update index when child parent_id changes")
        void findChildrenIdsByParentId_ChildParentChanged_UpdatesIndex() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));
            repository.save(new Transaction(2L, 200.0, "DEBIT", 1L));

            repository.save(new Transaction(2L, 200.0, "DEBIT", 3L));

            List<Long> result1 = repository.findChildrenIdsByParentId(1L);
            List<Long> result3 = repository.findChildrenIdsByParentId(3L);

            assertTrue(result1.isEmpty());
            assertEquals(1, result3.size());
            assertTrue(result3.contains(2L));
        }

        @Test
        @DisplayName("should remove child from old parent when overwritten")
        void findChildrenIdsByParentId_ChildOverwritten_RemovedFromOldParent() {
            repository.save(new Transaction(1L, 100.0, "CREDIT", null));
            repository.save(new Transaction(2L, 200.0, "DEBIT", 1L));
            repository.save(new Transaction(3L, 300.0, "CREDIT", 1L));

            repository.save(new Transaction(3L, 300.0, "CREDIT", 2L));

            List<Long> result1 = repository.findChildrenIdsByParentId(1L);
            List<Long> result2 = repository.findChildrenIdsByParentId(2L);

            assertEquals(1, result1.size());
            assertEquals(1, result2.size());
            assertTrue(result1.contains(2L));
            assertTrue(result2.contains(3L));
        }

        @Test
        @DisplayName("should maintain multiple levels of parent-child relationships")
        void findChildrenIdsByParentId_ChainHierarchy_WorksCorrectly() {
            repository.save(new Transaction(10L, 1000.0, "CREDIT", null));
            repository.save(new Transaction(11L, 100.0, "DEBIT", 10L));
            repository.save(new Transaction(12L, 200.0, "DEBIT", 11L));

            List<Long> childrenOf10 = repository.findChildrenIdsByParentId(10L);
            List<Long> childrenOf11 = repository.findChildrenIdsByParentId(11L);

            assertEquals(1, childrenOf10.size());
            assertTrue(childrenOf10.contains(11L));
            assertEquals(1, childrenOf11.size());
            assertTrue(childrenOf11.contains(12L));
        }
    }
}