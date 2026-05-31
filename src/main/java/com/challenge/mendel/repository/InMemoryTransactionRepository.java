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
    private final Map<Long, List<Long>> parentToChildren = new ConcurrentHashMap<>();

    @Override
    public void save(Transaction transaction) {
        if (transactions.containsKey(transaction.getId())) {
            Transaction existing = transactions.get(transaction.getId());
            removeFromTypeIndex(existing);
            removeFromParentChildIndex(existing);
        }
        transactions.put(transaction.getId(), transaction);
        addToTypeIndex(transaction);
        addToParentChildIndex(transaction);
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

    @Override
    public List<Long> findChildrenIdsByParentId(Long parentId) {
        if (parentId == null) {
            return List.of();
        }
        List<Long> children = parentToChildren.get(parentId);
        return children != null ? new ArrayList<>(children) : List.of();
    }

    private void addToParentChildIndex(Transaction transaction) {
        if (transaction.getParentId() != null) {
            parentToChildren.computeIfAbsent(transaction.getParentId(), k -> new ArrayList<>())
                    .add(transaction.getId());
        }
    }

    private void removeFromParentChildIndex(Transaction transaction) {
        if (transaction.getParentId() != null) {
            List<Long> children = parentToChildren.get(transaction.getParentId());
            if (children != null) {
                children.remove(transaction.getId());
            }
        }
    }
}