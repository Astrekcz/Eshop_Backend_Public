package org.example.eshopbackend.service;


import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.dto.AuthenticationRequestDTO;
import org.example.eshopbackend.dto.AuthenticationResponseDTO;
import org.example.eshopbackend.dto.CreateUserRequestDTO;
import org.example.eshopbackend.dto.UpdateUserRequestDTO;
import org.example.eshopbackend.entity.Role;
import org.example.eshopbackend.entity.User;
import org.example.eshopbackend.mapper.UserMapper;
import org.example.eshopbackend.repository.UserRepository;
import org.example.eshopbackend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserMapper userMapper;



    @Transactional
    public User register(CreateUserRequestDTO registerRequest) {
        // FE garantuje neprázdné hodnoty → jen normalizace
        String email = registerRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = registerRequest.getPhoneNumber() == null
                ? null
                : registerRequest.getPhoneNumber().trim().replaceAll("\\s+|-", "");

        // kontrola unikátu nad už normalizovanou hodnotou
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already taken");
        }
        if (phone != null && userRepository.findByPhoneNumber(phone).isPresent()) {
            throw new IllegalArgumentException("Phone number is already taken");
        }

        User newUser = userMapper.toUserEntity(registerRequest);
        newUser.setEmail(email);         // uložíš přesně to, co jsi kontroloval
        newUser.setPhoneNumber(phone);   // očistěný formát
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser.setRole(Role.USER);

        try {
            return userRepository.save(newUser);
        } catch (RuntimeException e) {
            log.error("Error saving user", e);  // loguj jako error, ne info
            throw e;                            // nevracej null
        }
    }

    public AuthenticationResponseDTO login(AuthenticationRequestDTO authenticationRequest) {
        String email = authenticationRequest.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtUtil.generateToken(userDetails, user.getUserID());
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
