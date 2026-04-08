package com.codeWithJeff.SampleSpringBootApplication.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


/**
 * Utility service that handles ALL JWT operations:
 *   1. generateToken()  — creates a signed JWT after successful login
 *   2. extractEmail()   — reads the "sub" (subject) claim from a token
 *   3. isTokenValid()   — checks signature + expiry
 *
 * FLOW:
 *   AuthController calls generateToken() → returns token string to client
 *   JwtAuthenticationFilter calls extractEmail() + isTokenValid() on every request
 */


@Service
@RequiredArgsConstructor
public class JwtService {


    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Creates the signing key from the secret string.
     * JJWT requires a SecretKey object, not a raw string.
     */

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a new JWT token for the given email.
     *
     * The token contains:
     *   - sub (subject): the user's email
     *   - iat (issued at): current time
     *   - exp (expiration): current time + 24 hours
     *   - signature: HMAC-SHA256 hash using our secret key
     */

    public String generateToken(String email, String role){
        return Jwts.builder()
                .subject(email) //who this token is for
                .claim("role",role) //custom claim
                .issuedAt(new Date()) //when it was created
                .expiration(new Date(System.currentTimeMillis() + expiration)) //when it expires
                .signWith(getSigningKey()) //sign with secret
                .compact(); //build the string
    }
    /**
     * Extract the email (subject claim) from a token.
     * Used by JwtAuthenticationFilter to identify which user is making the request.
     */
    public String extractEmail(String token){
        return extractAllClaims(token).getSubject();
    }

    /**
     * Check if a token is valid:
     *   1. Does the email in the token match the expected email?
     *   2. Has the token expired?
     */
    public boolean isTokenValid(String token, String expectedEmail){
        String email = extractEmail(token);
        return email.equals(expectedEmail) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token){
        return extractAllClaims(token).getExpiration().before(new Date());
    }


    /**
     * Parse and verify the token using our signing key.
     * If the signature doesn't match or token is malformed, JJWT throws an exception.
     */

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey()) //verify signature with our secret
                .build()
                .parseSignedClaims(token) //parse and validate
                .getPayload(); //return the claims (payload)
    }






}
