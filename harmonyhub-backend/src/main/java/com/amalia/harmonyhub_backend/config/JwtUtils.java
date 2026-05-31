package com.amalia.harmonyhub_backend.config;

import com.amalia.harmonyhub_backend.model.Role;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {
    private final String jwtSecret = "d341b0e4707806d076972bb00bef6869fc024814b2110828215db40588b41121";
    private final int jwtExpirationMs = 1000 * 60 * 60 * 24;

    private final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

    @Autowired
    private UserRepository userRepository;

    public String generateToken(String username) {
        User user = userRepository.findByUsername(username);
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        // Updated for 0.12.x: signWith(key) automatically detects the algorithm (HS256) based on key size
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return (List<String>) claims.get("roles");
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (JwtException e) {
            System.out.println("Invalid JWT: " + e.getMessage());
        }
        return false;
    }

    public String generatePartialToken(String username, String stepClaim) {
        return Jwts.builder()
                .subject(username)
                .claim("auth_step", stepClaim) // e.g., "AWAITING_MFA" or "AWAITING_QA"
                .claim("roles", List.of("ROLE_PRE_AUTH")) // Low-privilege role
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 300000)) // 5 minutes
                .signWith(key)
                .compact();
    }

    public String getStepFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("auth_step", String.class);
    }
}