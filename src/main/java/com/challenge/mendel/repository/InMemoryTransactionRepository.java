package com.challenge.mendel.repository;

import com.challenge.mendel.domain.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> transactions = new ConcurrentHashMap<>();
    private final Map<String, List<Transaction>> transactionsByType = new ConcurrentHashMap<>();

    @Override
    public void save(Transaction transaction) {
        if (transactions.containsKey(transaction.getId())) {
            Transaction existing = transactions.get(transaction.getId());
            removeFromTypeIndex(existing);
        }
        transactions.put(transaction.getId(), transaction);
        addToTypeIndex(transaction);
    }

    @Override
    public Transaction findById(Long id) {
        if (id == null) {
            return null;
        }
        return transactions.get(id);
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(transactions.values());
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        return transactions.containsKey(id);
    }

    @Override
    public List<Transaction> findByType(String type) {
        if (type == null || type.isBlank()) {
            return new ArrayList<>();
        }
        List<Transaction> result = transactionsByType.get(type);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    private void addToTypeIndex(Transaction transaction) {
        transactionsByType.computeIfAbsent(transaction.getType(), k -> new ArrayList<>())
                .add(transaction);
    }

    private void removeFromTypeIndex(Transaction transaction) {
        List<Transaction> list = transactionsByType.get(transaction.getType());
        if (list != null) {
            list.remove(transaction);
        }
    }
}