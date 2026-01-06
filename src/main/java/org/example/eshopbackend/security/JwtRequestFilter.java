package org.example.eshopbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.eshopbackend.service.CustomUserDetailsService;
import org.example.eshopbackend.util.JwtUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${public.endpoints:/api/auth/**}")
    private String[] publicEndpoints;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // nefiltruj preflight a HEAD (kvůli CORS a lightweight dotazům)
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }

        String uri = request.getRequestURI();
        String[] alwaysPublic = {
                "/images/**", "/css/**", "/js/**", "/static/**", "/favicon.ico", "/", "/error",
                "/files/**",
                "/api/catalog/**"
        };

        for (String pattern : publicEndpoints) {
            if (pathMatcher.match(pattern.trim(), uri)) return true;
        }
        for (String pattern : alwaysPublic) {
            if (pathMatcher.match(pattern, uri)) return true;
        }
        return false;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }


        final String authHeader = request.getHeader("Authorization");

        String jwt = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                log.warn("JWT Token is invalid: {}", e.getMessage());
            }
        }

// ... zůstává stejné ...

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails user = userDetailsService.loadUserByUsername(username);

            // 1) Sem dej UserDetails, ne String
            if (jwtUtil.validateToken(jwt, user)) {
                Claims claims = jwtUtil.extractAllClaims(jwt);

                // 2) Bez Optionalu – jednoduše:
                List<?> rawRoles = (List<?>) claims.get("roles");
                if (rawRoles == null) {
                    rawRoles = (List<?>) claims.get("role");
                }

                List<SimpleGrantedAuthority> authorities = (rawRoles == null ? List.<String>of() : rawRoles)
                        .stream()
                        .map(Object::toString)
                        .map(r -> r.replace('-', '_').trim())
                        .map(r -> {
                            if ("ADMIN".equals(r) || "ROLEADMIN".equals(r)) return "ROLE_ADMIN";
                            if (r.startsWith("ROLE_")) return r;
                            return "ROLE_" + r;
                        })
                        .distinct()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}