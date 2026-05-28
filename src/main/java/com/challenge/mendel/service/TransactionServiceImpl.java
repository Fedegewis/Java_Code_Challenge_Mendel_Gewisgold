package com.challenge.mendel.service;

import com.challenge.mendel.domain.Transaction;
import com.challenge.mendel.dto.UpdateTransactionRequest;
import com.challenge.mendel.exception.ParentTransactionNotFoundException;
import com.challenge.mendel.exception.ValidationException;
import com.challenge.mendel.repository.TransactionRepository;
import org.springframework.stereotype.Service;

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
            throw new ValidationException("Transaction cannot be its own parent");
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
}