package org.example.eshopbackend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final Key accessKey;

    public JwtUtil(@Value("${security.jwt.access-secret}") String accessSecret) {
        // žádné Base64 – bereš prostý text z properties
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        if (accessKey.getEncoded().length < 32) {
            throw new IllegalStateException("security.jwt.access-secret musí mít aspoň 32 bajtů");
        }
    }

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public Date extractExpiration(String token) { return extractClaim(token, Claims::getExpiration); }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)       // OVĚŘENÍ TÍM STEJNÝM KLÍČEM
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long extractUserId(String token) { return extractAllClaims(token).get("userId", Long.class); }

    private boolean isTokenExpired(String token) { return extractExpiration(token).before(new Date()); }

    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        // drž se jednoho klíče – máš ve filtru "role", tak ho necháme
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority()) // např. ROLE_ADMIN
                .collect(Collectors.toList());
        claims.put("role", roles);
        claims.put("userId", userId);
        claims.put("email", userDetails.getUsername());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 10))
                .signWith(accessKey, SignatureAlgorithm.HS256) // PODEPSÁNÍ TÍM STEJNÝM KLÍČEM
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
