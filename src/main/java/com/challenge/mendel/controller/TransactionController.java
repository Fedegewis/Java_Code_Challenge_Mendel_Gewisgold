package com.challenge.mendel.controller;

import com.challenge.mendel.dto.TransactionResponse;
import com.challenge.mendel.dto.UpdateTransactionRequest;
import com.challenge.mendel.exception.ValidationException;
import com.challenge.mendel.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PutMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> upsertTransaction(
            @PathVariable Long transactionId,
            @RequestBody UpdateTransactionRequest request) {

        if (request == null) {
            throw new ValidationException("Request body is required");
        }

        transactionService.upsertTransaction(transactionId, request);

        return ResponseEntity.ok(new TransactionResponse("ok"));
    }
}