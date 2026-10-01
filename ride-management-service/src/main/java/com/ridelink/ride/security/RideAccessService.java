package com.ridelink.ride.security;

import com.ridelink.ride.dto.DriverLookupResponse;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.service.DriverServiceClient;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RideAccessService {

    private final DriverServiceClient driverServiceClient;

    public RideAccessService(DriverServiceClient driverServiceClient) {
        this.driverServiceClient = driverServiceClient;
    }

    public Long accountId(Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bearer token is required"
            );
        }

        Object accountIdClaim =
                jwtAuthentication
                        .getToken()
                        .getClaim("accountId");

        // Preferred format: Account Service JWT contains accountId claim
        if (accountIdClaim != null) {
            try {
                long id = Long.parseLong(String.valueOf(accountIdClaim));

                if (id > 0) {
                    return id;
                }

            } catch (NumberFormatException exception) {
                // Continue to subject fallback.
            }
        }

        // Backward-compatible fallback for existing tests / older JWTs
        try {
            String subject =
                    jwtAuthentication
                            .getToken()
                            .getSubject();

            long id = Long.parseLong(subject);

            if (id > 0) {
                return id;
            }

        } catch (NumberFormatException | NullPointerException exception) {
            // Invalid token identity.
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid account ID in token"
        );
    }

    public void requirePassengerId(
            Long passengerId,
            Authentication authentication) {

        if (!Objects.equals(
                passengerId,
                accountId(authentication)
        )) {
            throw new AccessDeniedException(
                    "Passenger ID does not match the token account ID"
            );
        }
    }

    public void requirePassengerOrAdmin(
            Long passengerId,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            requirePassengerId(
                    passengerId,
                    authentication
            );
        }
    }

    public void requireRideOwnerOrAdmin(
            Ride ride,
            Authentication authentication) {

        requirePassengerOrAdmin(
                ride.getPassengerId(),
                authentication
        );
    }

    public void requireRideOwner(
            Ride ride,
            Authentication authentication) {

        requirePassengerId(
                ride.getPassengerId(),
                authentication
        );
    }

    public void requireAssignedDriverOrAdmin(
            Ride ride,
            Authentication authentication) {

        if (isAdmin(authentication)) {
            return;
        }

        if (authentication == null
                || authentication
                        .getAuthorities()
                        .stream()
                        .noneMatch(authority ->
                                "ROLE_DRIVER".equals(
                                        authority.getAuthority()
                                )
                        )) {

            throw new AccessDeniedException(
                    "DRIVER role is required"
            );
        }

        Long driverId = ride.getDriverId();

        if (driverId == null) {
            throw new AccessDeniedException(
                    "Ride has no assigned driver"
            );
        }

        DriverLookupResponse driver;

        try {
            driver =
                    driverServiceClient
                            .getDriver(driverId);

        } catch (RestClientException exception) {

            throw new AccessDeniedException(
                    "Assigned driver could not be verified",
                    exception
            );
        }

        if (driver == null
                || !Objects.equals(
                        driverId,
                        driver.id()
                )
                || !Objects.equals(
                        driver.accountId(),
                        accountId(authentication)
                )) {

            throw new AccessDeniedException(
                    "Driver does not own this ride"
            );
        }
    }

    private boolean isAdmin(Authentication authentication) {

        return authentication != null
                && authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                "ROLE_ADMIN".equals(
                                        authority.getAuthority()
                                )
                        );
    }
}