package com.ridelink.farepayment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RideFareServiceKeyFilter extends OncePerRequestFilter {
    private static final String HEADER = "X-Ride-Fare-Service-Key";
    private final byte[] expectedKey;

    public RideFareServiceKeyFilter(String serviceKey) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalArgumentException("RIDE_FARE_SERVICE_KEY must not be blank");
        }
        this.expectedKey = serviceKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Enumeration<String> values = request.getHeaders(HEADER);
        if (values.hasMoreElements()) {
            String suppliedKey = values.nextElement();
            if (!values.hasMoreElements() && MessageDigest.isEqual(expectedKey,
                    suppliedKey.getBytes(StandardCharsets.UTF_8))) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(
                        "ride-management-service", null,
                        List.of(new SimpleGrantedAuthority("ROLE_RIDE_FARE_SERVICE"))));
                SecurityContextHolder.setContext(context);
            }
        }
        chain.doFilter(request, response);
    }
}
