package org.example.eshopbackend.controllers;


import org.example.eshopbackend.exception.MissingCredentialsException;
import org.example.eshopbackend.exception.PplAccountMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MissingCredentialsException.class)
    public ResponseEntity<String> handleEmailConfigException(MissingCredentialsException ex){
        log.error(ex.getMessage());

        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(PplAccountMissingException.class)
    public ResponseEntity<Map<String, String>> handlePplMissing(PplAccountMissingException ex) {
        // Frontend dostane JSON: { "message": "Nemáte PPL účet (nebo jsou špatné přihlašovací údaje)" }
        // HTTP Status bude 400 Bad Request
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}