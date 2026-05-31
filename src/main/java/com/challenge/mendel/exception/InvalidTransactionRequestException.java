package com.challenge.mendel.exception;

public class InvalidTransactionRequestException extends RuntimeException {

    public InvalidTransactionRequestException(String message) {
        super(message);
    }
}