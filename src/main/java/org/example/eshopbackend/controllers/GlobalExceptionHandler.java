package org.example.eshopbackend.controllers;


import org.example.eshopbackend.exception.PplAccountMissingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Toto zachytí naši speciální chybu z PplTokenService
     * a pošle na Frontend JSON s kódem 400.
     */
    @ExceptionHandler(PplAccountMissingException.class)
    public ResponseEntity<Map<String, String>> handlePplMissing(PplAccountMissingException ex) {
        // Frontend dostane JSON: { "message": "Nemáte PPL účet (nebo jsou špatné přihlašovací údaje)" }
        // HTTP Status bude 400 Bad Request
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}