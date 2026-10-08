package com.realestate.service;

import com.realestate.model.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final String encodedSecret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret-base64:}") String encodedSecret,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        this.encodedSecret = encodedSecret;
        this.expirationSeconds = expirationSeconds;
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero seconds.");
        }
    }

    public IssuedToken issueToken(String email, RoleName role) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);
        String token = Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    public TokenIdentity parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new TokenIdentity(claims.getSubject(), RoleName.valueOf(claims.get("role", String.class)));
    }

    private SecretKey signingKey() {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT signing secret is not configured. Set the JWT_SECRET environment variable.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(encodedSecret);
        } catch (DecodingException exception) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64-encoded key.", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to a key of at least 32 bytes.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public record TokenIdentity(String email, RoleName role) {
    }
}
