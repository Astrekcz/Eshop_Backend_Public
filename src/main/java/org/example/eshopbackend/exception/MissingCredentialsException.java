package org.example.eshopbackend.exception;

public class MissingCredentialsException extends RuntimeException {
    public MissingCredentialsException(String message) {
        super(message);
    }
}
