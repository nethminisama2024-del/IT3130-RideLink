package com.ridelink.account_service.security;

import com.ridelink.account_service.entity.User;
import com.ridelink.account_service.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void generatedTokenShouldContainRideLinkIssuerAndRequiredClaims() {
        String secret = "RideLinkTestJwtSecret_2026_123456789012345";

        JwtService jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        User user = User.builder()
                .id(1L)
                .email("passenger@test.com")
                .role(Role.PASSENGER)
                .build();

        String token = jwtService.generateToken(user);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("RideLink", claims.getIssuer());
        assertEquals("passenger@test.com", claims.getSubject());
        assertEquals("1", claims.get("accountId").toString());
        assertEquals("PASSENGER", claims.get("role", String.class));
    }
}
