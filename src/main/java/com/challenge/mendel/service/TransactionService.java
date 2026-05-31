package com.challenge.mendel.service;

import com.challenge.mendel.dto.UpdateTransactionRequest;
import java.util.List;

public interface TransactionService {

    void upsertTransaction(Long transactionId, UpdateTransactionRequest request);

    List<Long> getTransactionIdsByType(String type);

    List<Long> getChildrenIdsByParentId(Long parentId);

    Double getTransactionSum(Long transactionId);
}