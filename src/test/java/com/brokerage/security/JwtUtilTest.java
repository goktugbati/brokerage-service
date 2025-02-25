package com.brokerage.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private UserDetails userDetails;
    private String testSecret = "thisIsATestSecretKeyThatIsLongEnoughForHmacSha256Algorithm";
    private Long testExpiration = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);
        jwtUtil.init(); // Initialize the key

        userDetails = new User(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        String token = jwtUtil.generateToken(userDetails);

        assertNotNull(token);
        assertTrue(token.length() > 0);

        String username = jwtUtil.extractUsername(token);
        assertEquals("testuser", username);

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtUtil.generateToken(userDetails);

        boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void validateToken_WithDifferentUser_ShouldReturnFalse() {
        String token = jwtUtil.generateToken(userDetails);

        UserDetails differentUser = new User(
                "differentuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        boolean isValid = jwtUtil.validateToken(token, differentUser);

        assertFalse(isValid);
    }

    @Test
    void validateToken_WithExpiredToken_ShouldThrowException() {
        Key key = (Key) ReflectionTestUtils.getField(jwtUtil, "key");

        Date expiration = new Date(System.currentTimeMillis() - 3600000);

        String token = Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expiration)
                .signWith(key)
                .compact();

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            jwtUtil.extractUsername(token);
        });
    }

    @Test
    void extractClaim_ShouldReturnCorrectClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("testKey", "testValue");

        Key key = (Key) ReflectionTestUtils.getField(jwtUtil, "key");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + testExpiration))
                .signWith(key)
                .compact();

        String claimValue = jwtUtil.extractClaim(token, claimsMap -> claimsMap.get("testKey", String.class));

        assertEquals("testValue", claimValue);
    }

    @Test
    void extractExpiration_ShouldReturnCorrectExpirationDate() {
        Date expectedExpiration = new Date(System.currentTimeMillis() + testExpiration);
        String token = jwtUtil.generateToken(userDetails);

        Date extractedExpiration = jwtUtil.extractExpiration(token);

        long differenceInMillis = Math.abs(expectedExpiration.getTime() - extractedExpiration.getTime());
        assertTrue(differenceInMillis < 1000, "Expiration dates should be within 1 second of each other");
    }

    @Test
    void extractAllClaims_ShouldExtractCorrectClaims() {
        String token = jwtUtil.generateToken(userDetails);

        Claims claims = (Claims) ReflectionTestUtils.invokeMethod(jwtUtil, "extractAllClaims", token);

        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void validateToken_WithInvalidSignature_ShouldThrowException() {
        String differentSecret = "differentSecretKeyUsedForSigningThisTokenThatWontMatch";
        ReflectionTestUtils.setField(jwtUtil, "secret", differentSecret);
        jwtUtil.init();
        String token = jwtUtil.generateToken(userDetails);

        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        jwtUtil.init();

        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> {
            jwtUtil.extractUsername(token);
        });
    }

    @Test
    void validateToken_WithSignatureException_ShouldThrowException() {
        // This token has a valid structure but invalid signature
        String tokenWithInvalidSignature = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.INVALID-MIDDLE-PART.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> {
            jwtUtil.extractUsername(tokenWithInvalidSignature);
        });
    }

    @Test
    void validateToken_WithTrulyMalformedToken_ShouldThrowException() {
        String trulyMalformedToken = "not-a-valid-jwt-token-at-all";

        assertThrows(io.jsonwebtoken.MalformedJwtException.class, () -> {
            jwtUtil.extractUsername(trulyMalformedToken);
        });
    }
}