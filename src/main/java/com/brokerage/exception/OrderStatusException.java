package com.brokerage.exception;

public class OrderStatusException extends RuntimeException {
    public OrderStatusException(String message) {
        super(message);
    }
}
