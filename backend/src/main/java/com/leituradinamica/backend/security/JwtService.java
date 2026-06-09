package com.leituradinamica.backend.security;

import com.leituradinamica.backend.config.AppProperties;
import com.leituradinamica.backend.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generateAccessToken(User user) {
        Instant expiresAt = Instant.now().plus(appProperties.jwt().accessTokenExpirationMinutes(), ChronoUnit.MINUTES);
        return buildToken(user, expiresAt);
    }

    public String generateRefreshToken(User user) {
        Instant expiresAt = Instant.now().plus(appProperties.jwt().refreshTokenExpirationDays(), ChronoUnit.DAYS);
        return buildToken(user, expiresAt);
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public boolean isTokenValid(String token, User user) {
        return extractUsername(token).equals(user.getEmail()) && extractExpiration(token).isAfter(Instant.now());
    }

    private String buildToken(User user, Instant expiresAt) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(appProperties.jwt().secret().getBytes())));
    }
}