package com.challenge.mendel.exception;

public class ParentTransactionNotFoundException extends RuntimeException {

    public ParentTransactionNotFoundException(Long parentId) {
        super("Parent transaction with id " + parentId + " not found");
    }
}