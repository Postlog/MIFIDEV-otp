package com.promoit.otp.security;

import com.promoit.otp.config.AppConfig;
import com.promoit.otp.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies signed JWT access tokens (HMAC-SHA256). A token carries
 * the user id, login and role, plus an expiration claim, so authorisation is
 * fully stateless.
 */
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final SecretKey key;
    private final long ttlSeconds;
    private final String issuer;

    public TokenService(AppConfig config) {
        String secret = config.get("jwt.secret");
        if (secret == null || secret.isBlank()) {
            // Fail-safe: never rely on a committed/default signing key. When none is
            // supplied we generate a random, ephemeral secret for this run only, so
            // tokens can never be forged from repository contents. Set jwt.secret /
            // JWT_SECRET for a stable key that survives restarts.
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("jwt.secret is not configured — generated a random ephemeral signing key. "
                    + "Tokens will be invalidated on restart. Set JWT_SECRET for a stable key.");
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes long for HMAC-SHA256");
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.ttlSeconds = config.getLong("jwt.ttl.seconds", 3600);
        this.issuer = config.get("jwt.issuer", "mifidev-otp");
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    /** Builds a signed token for the given identity. */
    public String generateToken(long userId, String login, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .issuer(issuer)
                .subject(login)
                .claim("uid", userId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Instant expiryFromNow() {
        return Instant.now().plusSeconds(ttlSeconds);
    }

    /**
     * Verifies the token signature and expiration and returns the principal.
     *
     * @throws InvalidTokenException if the token is malformed, tampered with or expired
     */
    public AuthPrincipal verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number userId = claims.get("uid", Number.class);
            String roleClaim = claims.get("role", String.class);
            if (userId == null || roleClaim == null) {
                throw new InvalidTokenException("Token is missing required claims");
            }
            return new AuthPrincipal(userId.longValue(), claims.getSubject(), Role.valueOf(roleClaim));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token verification failed: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    /** Thrown when a presented token cannot be trusted. */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
