package org.example.eshopbackend.security;

import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.repository.UserRepository; // Předpokládám existenci repozitáře
import org.example.eshopbackend.service.CustomUserDetailsService; // Nebo použijeme přímo repo viz níže
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final CustomUserDetailsService customUserDetailsService;

    // 1. Definice UserDetailsService - říkáme Springu, jak najít uživatele
    @Bean
    public UserDetailsService userDetailsService() {
        return customUserDetailsService;
    }

    // 2. Definice AuthenticationProvider - tohle jsi chtěl explicitně
    // Spojuje UserDetailsService a PasswordEncoder
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // 3. AuthenticationManager - používá se v LoginService k samotnému přihlášení
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 4. PasswordEncoder - globální instance
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}