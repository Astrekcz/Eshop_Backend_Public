package org.example.eshopbackend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.dto.verification.SendCodeRequest;
import org.example.eshopbackend.dto.verification.VerifyCodeRequest;
import org.example.eshopbackend.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendCode(@Valid @RequestBody SendCodeRequest request) {
        verificationService.sendCode(request.getEmail());
        return ResponseEntity.ok(Collections.singletonMap("message", "Kód byl odeslán."));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        boolean isValid = verificationService.verifyCode(request.getEmail(), request.getCode());

        if (isValid) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Email úspěšně ověřen."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Neplatný nebo expirovaný kód."));
        }
    }
}