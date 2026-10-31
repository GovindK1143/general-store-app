package com.storeservice.exception;

public class StoreEmployeeNotFoundException
        extends RuntimeException {

    public StoreEmployeeNotFoundException(String message) {
        super(message);
    }
}