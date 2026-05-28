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
}