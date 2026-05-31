package com.challenge.mendel.service;

import com.challenge.mendel.domain.Transaction;
import com.challenge.mendel.dto.UpdateTransactionRequest;
import com.challenge.mendel.exception.InvalidParentTransactionException;
import com.challenge.mendel.exception.InvalidTransactionRequestException;
import com.challenge.mendel.exception.ParentTransactionNotFoundException;
import com.challenge.mendel.exception.TransactionNotFoundException;
import com.challenge.mendel.exception.ValidationException;
import com.challenge.mendel.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;

    public TransactionServiceImpl(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void upsertTransaction(Long transactionId, UpdateTransactionRequest request) {
        validateRequest(transactionId, request);

        if (request.getParentId() != null && request.getParentId().equals(transactionId)) {
            throw new InvalidParentTransactionException("Transaction cannot be its own parent");
        }

        if (request.getParentId() != null && !repository.existsById(request.getParentId())) {
            throw new ParentTransactionNotFoundException(request.getParentId());
        }

        Transaction transaction = new Transaction(
                transactionId,
                request.getAmount(),
                request.getType(),
                request.getParentId()
        );

        repository.save(transaction);
    }

    private void validateRequest(Long transactionId, UpdateTransactionRequest request) {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID is required");
        }
        if (request.getAmount() == null) {
            throw new ValidationException("Amount is required");
        }
        if (request.getAmount() <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new ValidationException("Type is required");
        }
    }

    @Override
    public List<Long> getTransactionIdsByType(String type) {
        if (type == null || type.isBlank()) {
            return List.of();
        }
        List<Transaction> transactions = repository.findByType(type);
        if (transactions == null) {
            return List.of();
        }
        return transactions.stream()
                .map(Transaction::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getChildrenIdsByParentId(Long parentId) {
        if (parentId == null) {
            return List.of();
        }
        return repository.findChildrenIdsByParentId(parentId);
    }

    @Override
    public Double getTransactionSum(Long transactionId) {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID is required");
        }

        Transaction transaction = repository.findById(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException(transactionId);
        }

        return calculateSum(transaction);
    }

    private Double calculateSum(Transaction transaction) {
        Double sum = transaction.getAmount();

        List<Long> childrenIds = repository.findChildrenIdsByParentId(transaction.getId());
        for (Long childId : childrenIds) {
            Transaction child = repository.findById(childId);
            if (child != null) {
                sum += calculateSum(child);
            }
        }

        return sum;
    }
}