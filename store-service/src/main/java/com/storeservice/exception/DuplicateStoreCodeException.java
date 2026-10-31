package com.storeservice.exception;

public class DuplicateStoreCodeException extends RuntimeException {

    public DuplicateStoreCodeException(String message) {
        super(message);
    }
}