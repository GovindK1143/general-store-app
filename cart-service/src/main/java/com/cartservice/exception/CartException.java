package com.cartservice.exception;

public class CartException extends RuntimeException {

    public CartException(String message) {
        super(message);
    }
}