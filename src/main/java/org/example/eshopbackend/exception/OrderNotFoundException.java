package org.example.eshopbackend.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("Order with id " + id + " not found");
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}
