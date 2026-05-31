package com.challenge.mendel.exception;

public class InvalidParentTransactionException extends RuntimeException {

    public InvalidParentTransactionException(String message) {
        super(message);
    }

    public InvalidParentTransactionException(Long transactionId, Long parentId) {
        super("Transaction " + transactionId + " cannot have " + parentId + " as parent");
    }
}