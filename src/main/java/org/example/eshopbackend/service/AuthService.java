package org.example.eshopbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.AuthenticationRequestDTO;
import org.example.eshopbackend.dto.AuthenticationResponseDTO;
import org.example.eshopbackend.dto.CreateUserRequestDTO;
import org.example.eshopbackend.dto.UpdateUserRequestDTO;
import org.example.eshopbackend.entity.Role;
import org.example.eshopbackend.entity.User;
import org.example.eshopbackend.mapper.UserMapper;
import org.example.eshopbackend.repository.UserRepository;
import org.example.eshopbackend.security.CustomUserDetails; // Import nového UserDetails
import org.example.eshopbackend.security.JwtService;       // Import nové Service
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService; // Změna z JwtUtil na JwtService
    private final AuthenticationManager authenticationManager; // Nová komponenta pro login
    private final UserMapper userMapper;

    @Transactional
    public User register(CreateUserRequestDTO registerRequest) {
        String email = registerRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = registerRequest.getPhoneNumber() == null
                ? null
                : registerRequest.getPhoneNumber().trim().replaceAll("\\s+|-", "");

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already taken");
        }
        if (phone != null && userRepository.findByPhoneNumber(phone).isPresent()) {
            throw new IllegalArgumentException("Phone number is already taken");
        }

        User newUser = userMapper.toUserEntity(registerRequest);
        newUser.setEmail(email);
        newUser.setPhoneNumber(phone);
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser.setRole(Role.USER);

        try {
            return userRepository.save(newUser);
        } catch (RuntimeException e) {
            log.error("Error saving user", e);
            throw e;
        }
    }

    public AuthenticationResponseDTO login(AuthenticationRequestDTO request) {
        // 1. Normalizace emailu (TOHLE TAM CHYBĚLO)
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // 2. AuthenticationManager - použijeme už normalizovaný email
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email, // Zde použij proměnnou 'email', ne 'request.getEmail()'
                        request.getPassword()
                )
        );

        // 3. Načtení z DB - použijeme normalizovaný email
        User user = userRepository.findByEmail(email) // Zde použij proměnnou 'email'
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // ... zbytek kódu (CustomUserDetails, generování tokenu) ...

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getUserID());
        extraClaims.put("role", "ROLE_" + user.getRole().name());

        String jwtToken = jwtService.generateToken(extraClaims, userDetails);

        return new AuthenticationResponseDTO(jwtToken);
    }

    @Transactional
    public User updateUser(UpdateUserRequestDTO dto, Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (dto.getEmail() != null) {
            String normalized = dto.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!normalized.equalsIgnoreCase(u.getEmail())
                    && userRepository.findByEmail(normalized).isPresent()) {
                throw new IllegalArgumentException("Email is already taken");
            }
            u.setEmail(normalized);
        }

        if (dto.getPhoneNumber() != null) {
            String phone = dto.getPhoneNumber().trim().replaceAll("\\s+|-", "");
            if (!phone.equals(u.getPhoneNumber())
                    && userRepository.findByPhoneNumber(phone).isPresent()) {
                throw new IllegalArgumentException("Phone number is already taken");
            }
            u.setPhoneNumber(phone);
        }

        userMapper.updateUser(u, dto);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        try {
            return userRepository.save(u);
        } catch (RuntimeException e) {
            log.error("Error updating user", e);
            throw e;
        }
    }
}