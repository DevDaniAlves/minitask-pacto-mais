package com.example.minitask_pacto_mais.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.minitask_pacto_mais.domain.Role;
import com.example.minitask_pacto_mais.domain.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Algorithm algorithm;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC256(properties.secret());
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(properties.expirationMs());

        return JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("name", user.getName())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public String generateTempToken(User user, String purpose, long ttlMs) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("purpose", purpose)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(ttlMs)))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(algorithm)
                .withIssuer(properties.issuer())
                .build()
                .verify(token);
    }

    public UUID extractUserId(DecodedJWT jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public Role extractRole(DecodedJWT jwt) {
        return Role.valueOf(jwt.getClaim("role").asString());
    }
}