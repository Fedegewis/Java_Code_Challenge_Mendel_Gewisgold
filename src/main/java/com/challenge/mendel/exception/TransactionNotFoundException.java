package com.challenge.mendel.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction with id " + transactionId + " not found");
    }
}