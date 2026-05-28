package com.challenge.mendel.repository;

import com.challenge.mendel.domain.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> transactions = new ConcurrentHashMap<>();

    @Override
    public void save(Transaction transaction) {
        transactions.put(transaction.getId(), transaction);
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
}