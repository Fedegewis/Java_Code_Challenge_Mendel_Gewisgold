package com.challenge.mendel.service;

import com.challenge.mendel.dto.UpdateTransactionRequest;

public interface TransactionService {

    void upsertTransaction(Long transactionId, UpdateTransactionRequest request);
}