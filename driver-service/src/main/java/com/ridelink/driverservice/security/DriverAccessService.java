package com.ridelink.driverservice.security;

import com.ridelink.driverservice.dto.AvailabilityRequest;
import com.ridelink.driverservice.entity.Driver;
import com.ridelink.driverservice.enums.AvailabilityStatus;
import com.ridelink.driverservice.service.DriverService;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class DriverAccessService {
    private final DriverService driverService;

    public DriverAccessService(DriverService driverService) {
        this.driverService = driverService;
    }

    public void requireCreateProfile(Long accountId, Authentication authentication) {
        if (hasRole(authentication, "ADMIN")) {
            return;
        }
        if (!hasRole(authentication, "DRIVER") || !Objects.equals(accountId, accountId(authentication))) {
            throw new AccessDeniedException("Driver profile account ID does not match the token");
        }
    }

    public Driver requireDriverRead(Long driverId, Authentication authentication) {
        if (hasRole(authentication, "RIDE_SERVICE")) {
            return driverService.getDriver(driverId);
        }
        return requireOwnerOrAdmin(driverId, authentication);
    }

    public Driver requireOwnerOrAdmin(Long driverId, Authentication authentication) {
        Driver driver = driverService.getDriver(driverId);
        if (hasRole(authentication, "ADMIN")) {
            return driver;
        }
        if (!hasRole(authentication, "DRIVER")
                || !Objects.equals(driver.getAccountId(), accountId(authentication))) {
            throw new AccessDeniedException("Driver does not own this profile");
        }
        return driver;
    }

    public void requireAvailabilityChange(Long driverId, AvailabilityRequest request,
                                          Authentication authentication) {
        if (hasRole(authentication, "RIDE_SERVICE") || hasRole(authentication, "ADMIN")) {
            return;
        }
        Driver driver = requireOwnerOrAdmin(driverId, authentication);
        if (driver.getAvailability() == AvailabilityStatus.ON_RIDE
                || request.getAvailability() == AvailabilityStatus.ON_RIDE) {
            throw new AccessDeniedException("Only Ride Management or ADMIN may change ride availability");
        }
    }

    private Long accountId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AccessDeniedException("A driver JWT is required");
        }
        try {
            long id = Long.parseLong(jwtAuthentication.getToken().getSubject());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException exception) {
            // A malformed JWT subject cannot establish driver ownership.
        }
        throw new AccessDeniedException("Invalid account ID in token");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }
}
