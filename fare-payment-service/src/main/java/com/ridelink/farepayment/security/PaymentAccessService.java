package com.ridelink.farepayment.security;

import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class PaymentAccessService {
    public boolean canRead(Long passengerId, Authentication authentication) {
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }
        if (passengerId == null || !hasRole(authentication, "PASSENGER")
                || !(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }
        try {
            long accountId = Long.parseLong(jwtAuthentication.getToken().getSubject());
            return accountId > 0 && Objects.equals(passengerId, accountId);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }
}
