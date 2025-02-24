package com.brokerage.exception;

public class InsufficientAssetsException extends RuntimeException {
    public InsufficientAssetsException(String message) {
        super(message);
    }
}
