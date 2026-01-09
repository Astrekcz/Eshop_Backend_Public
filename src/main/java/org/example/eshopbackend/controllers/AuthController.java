package org.example.eshopbackend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.AuthenticationRequestDTO;
import org.example.eshopbackend.dto.AuthenticationResponseDTO;
import org.example.eshopbackend.dto.CreateUserRequestDTO;
import org.example.eshopbackend.dto.UpdateUserRequestDTO;
import org.example.eshopbackend.security.CustomUserDetails;
import org.example.eshopbackend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    // JwtUtil/JwtService zde už není potřeba, controller ho nepoužívá

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody CreateUserRequestDTO registerRequest) {
        try {
            authService.register(registerRequest);
            return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // Specifický catch pro validace (např. email existuje)
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequestDTO authenticationRequest) {
        try {
            AuthenticationResponseDTO response = authService.login(authenticationRequest);
            log.info("User logged in: {}", authenticationRequest.getEmail());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // AuthenticationManager vyhodí výjimku, pokud jsou údaje špatně
            log.warn("Login failed for {}: {}", authenticationRequest.getEmail(), e.getMessage());
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<String> updateMe(
            // Magie Spring Security: Injektne aktuálně přihlášeného uživatele (z tokenu)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUserRequestDTO dto) {

        try {
            // 1. Bezpečnostní pojistka (i když SecurityConfig by to sem bez tokenu nepustil)
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            // 2. Získání ID přímo z načteného kontextu
            Long userId = userDetails.getUserId();

            // 3. Update
            authService.updateUser(dto, userId);

            return ResponseEntity.ok("User updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}