package com.storeservice.exception;

public class EmployeeAssignmentConflictException
        extends RuntimeException {

    public EmployeeAssignmentConflictException(String message) {
        super(message);
    }
}