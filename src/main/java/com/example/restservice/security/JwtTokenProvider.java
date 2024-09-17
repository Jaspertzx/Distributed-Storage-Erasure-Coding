/**
 * This class provides methods for generating, validating, and extracting information from JWT tokens.
 * It uses the HS512 signature algorithm and a secret key to sign and verify JWT tokens.
 * The class includes functionality for generating tokens with expiration, validating tokens, and
 * extracting the username (subject) from a JWT.
 * Dependencies:
 * - io.jsonwebtoken: Library used for creating and parsing JWT tokens.
 * - javax.crypto: Used to create the secret key for signing the JWT.
 * Configurations:
 * - JWT_SECRET: Secret key used for signing the JWT, configured through application properties.
 * - JWT_EXPIRATION_MS: Token expiration time set to 24 hours (in milliseconds).
 * Author: Jasper Tan
 */
package com.example.restservice.security;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String JWT_SECRET;

    private final long JWT_EXPIRATION_MS = 86400000;

    /**
     * Generates a JWT token for a given username.
     * The token is signed with a secret key and includes an expiration time of 24 hours.
     *
     * @param username The username to include in the JWT token.
     * @return A JWT token as a String.
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION_MS);

        SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS512.getJcaName());

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();
    }

    /**
     * Validates a JWT token by checking its signature and expiration.
     *
     * @param token The JWT token to validate.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS512.getJcaName());
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Invalid JWT Token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the username (subject) from a JWT token.
     * The username is stored as the 'subject' in the token claims.
     *
     * @param token The JWT token from which to extract the username.
     * @return The username contained in the token.
     */
    public String getUsernameFromJWT(String token) {
        SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS512.getJcaName());

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }
}
