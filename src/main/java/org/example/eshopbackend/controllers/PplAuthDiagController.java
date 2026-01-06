// src/main/java/org/example/zeniqbackend/controllers/PplAuthDiagController.java
package org.example.eshopbackend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.shipping.ppl.auth.PplTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping/ppl/_diag")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PplAuthDiagController {

    private final PplTokenService tokenService;


    @GetMapping("/token")
    public ResponseEntity<String> token() {
        String t = tokenService.getAccessToken();
        String masked = t.length() <= 12 ? t : t.substring(0, 6) + "..." + t.substring(t.length() - 6);
        return ResponseEntity.ok("OK token: " + masked);
    }




}
