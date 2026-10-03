package com.ridelink.ride.controller;

import com.ridelink.ride.dto.PaymentResponse;
import com.ridelink.ride.dto.PaymentRideRequest;
import com.ridelink.ride.dto.DriverLookupResponse;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.service.RideService;
import com.ridelink.ride.service.DriverServiceClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RideControllerSecurityTest {
    private static final String SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final JwtEncoder ENCODER = NimbusJwtEncoder.withSecretKey(
            new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> SECRET);
        registry.add("driver.service.key", () -> UUID.randomUUID().toString());
        registry.add("ride.fare.service.key", () -> UUID.randomUUID().toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:ride_security_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean RideService rideService;
    @MockitoBean DriverServiceClient driverServiceClient;

    @Test
    void passengerCanCreateOwnRide() throws Exception {
        when(rideService.createRide(any())).thenReturn(ride(12L, RideStatus.REQUESTED));

        mockMvc.perform(post("/api/rides").header("Authorization", bearer(12L, "PASSENGER"))
                        .contentType("application/json").content(createBody(12L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passengerId").value(12));

        verify(rideService).createRide(any());
    }

    @Test
    void passengerCannotCreateRideForAnotherAccount() throws Exception {
        mockMvc.perform(post("/api/rides").header("Authorization", bearer(12L, "PASSENGER"))
                        .contentType("application/json").content(createBody(13L)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rideService);
    }

    @Test
    void passengerCanGetOwnRide() throws Exception {
        when(rideService.getRide(5L)).thenReturn(ride(12L, RideStatus.REQUESTED));

        mockMvc.perform(get("/api/rides/5").header("Authorization", bearer(12L, "PASSENGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengerId").value(12));
    }

    @Test
    void passengerCannotGetAnotherPassengersRide() throws Exception {
        when(rideService.getRide(5L)).thenReturn(ride(13L, RideStatus.REQUESTED));

        mockMvc.perform(get("/api/rides/5").header("Authorization", bearer(12L, "PASSENGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void passengerCannotListAnotherPassengersRides() throws Exception {
        mockMvc.perform(get("/api/rides").param("passengerId", "13")
                        .header("Authorization", bearer(12L, "PASSENGER")))
                .andExpect(status().isForbidden());

        verify(rideService, never()).getPassengerRides(any());
    }

    @Test
    void passengerCannotAssignAnotherPassengersRide() throws Exception {
        when(rideService.getRide(5L)).thenReturn(ride(13L, RideStatus.REQUESTED));

        mockMvc.perform(patch("/api/rides/5/assign")
                        .header("Authorization", bearer(12L, "PASSENGER"))
                        .contentType("application/json").content("{\"serviceArea\":\"Colombo\"}"))
                .andExpect(status().isForbidden());

        verify(rideService, never()).assignDriver(eq(5L), any());
    }

    @Test
    void passengerCannotCancelAnotherPassengersRide() throws Exception {
        when(rideService.getRide(5L)).thenReturn(ride(13L, RideStatus.REQUESTED));

        mockMvc.perform(patch("/api/rides/5/cancel")
                        .header("Authorization", bearer(12L, "PASSENGER")))
                .andExpect(status().isForbidden());

        verify(rideService, never()).cancelRide(5L);
    }

    @Test
    void passengerCannotPayAnotherPassengersRide() throws Exception {
        when(rideService.getRide(5L)).thenReturn(ride(13L, RideStatus.COMPLETED));

        mockMvc.perform(post("/api/rides/5/payment")
                        .header("Authorization", bearer(12L, "PASSENGER"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());

        verify(rideService, never()).createPayment(eq(5L), any(PaymentRideRequest.class));
    }

    @Test
    void passengerCanPayOwnCompletedRide() throws Exception {
        when(rideService.getRide(5L)).thenReturn(ride(12L, RideStatus.COMPLETED));
        when(rideService.createPayment(eq(5L), any(PaymentRideRequest.class)))
                .thenReturn(new PaymentResponse(7L, 5L, 12L, 100.0, "SIMULATED_CARD",
                        "SUCCESS", "ref", "now"));

        mockMvc.perform(post("/api/rides/5/payment")
                        .header("Authorization", bearer(12L, "PASSENGER"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passengerId").value(12));
    }

    @Test
    void adminCanReadListAssignAndCancelAnyPassengerRide() throws Exception {
        Ride ride = ride(13L, RideStatus.REQUESTED);
        when(rideService.getRide(5L)).thenReturn(ride);
        when(rideService.getPassengerRides(13L)).thenReturn(List.of(ride));
        when(rideService.assignDriver(eq(5L), any())).thenReturn(ride);
        when(rideService.cancelRide(5L)).thenReturn(ride);
        String admin = bearer(99L, "ADMIN");

        mockMvc.perform(get("/api/rides/5").header("Authorization", admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rides").param("passengerId", "13")
                        .header("Authorization", admin))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/rides/5/assign").header("Authorization", admin)
                        .contentType("application/json").content("{\"serviceArea\":\"Colombo\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/rides/5/cancel").header("Authorization", admin))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanUseLifecycleRouteWithoutDriverLookup() throws Exception {
        when(rideService.getRide(5L)).thenReturn(ride(13L, RideStatus.ASSIGNED));
        when(rideService.acceptRide(5L)).thenReturn(ride(13L, RideStatus.ACCEPTED));

        mockMvc.perform(patch("/api/rides/5/accept")
                        .header("Authorization", bearer(99L, "ADMIN")))
                .andExpect(status().isOk());

        verifyNoInteractions(driverServiceClient);
    }

    @Test
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/rides/5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/rides/5").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/rides").header("Authorization", bearer(12L, "DRIVER"))
                        .contentType("application/json").content(createBody(12L)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rideService);
    }

    @Test
    void assignedDriverCanAccept() throws Exception {
        Ride ride = assignedRide(RideStatus.ASSIGNED);
        when(rideService.getRide(5L)).thenReturn(ride);
        when(driverServiceClient.getDriver(42L)).thenReturn(new DriverLookupResponse(42L, 12L));
        when(rideService.acceptRide(5L)).thenReturn(ride(13L, RideStatus.ACCEPTED));

        mockMvc.perform(patch("/api/rides/5/accept")
                        .header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isOk());

        verify(driverServiceClient).getDriver(42L);
        verify(rideService).acceptRide(5L);
    }

    @Test
    void assignedDriverCanStart() throws Exception {
        when(rideService.getRide(5L)).thenReturn(assignedRide(RideStatus.ACCEPTED));
        when(driverServiceClient.getDriver(42L)).thenReturn(new DriverLookupResponse(42L, 12L));
        when(rideService.startRide(5L)).thenReturn(assignedRide(RideStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/rides/5/start")
                        .header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isOk());

        verify(rideService).startRide(5L);
    }

    @Test
    void assignedDriverCanComplete() throws Exception {
        when(rideService.getRide(5L)).thenReturn(assignedRide(RideStatus.IN_PROGRESS));
        when(driverServiceClient.getDriver(42L)).thenReturn(new DriverLookupResponse(42L, 12L));
        when(rideService.completeRide(eq(5L), any())).thenReturn(assignedRide(RideStatus.COMPLETED));

        mockMvc.perform(patch("/api/rides/5/complete")
                        .header("Authorization", bearer(12L, "DRIVER"))
                        .contentType("application/json").content("{\"distanceKm\":5.0}"))
                .andExpect(status().isOk());

        verify(rideService).completeRide(eq(5L), any());
    }

    @Test
    void unrelatedDriverCannotAccept() throws Exception {
        when(rideService.getRide(5L)).thenReturn(assignedRide(RideStatus.ASSIGNED));
        when(driverServiceClient.getDriver(42L)).thenReturn(new DriverLookupResponse(42L, 99L));

        mockMvc.perform(patch("/api/rides/5/accept")
                        .header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isForbidden());

        verify(rideService, never()).acceptRide(5L);
    }

    @Test
    void passengerCannotAccept() throws Exception {
        mockMvc.perform(patch("/api/rides/5/accept")
                        .header("Authorization", bearer(13L, "PASSENGER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(driverServiceClient);
    }

    @Test
    void driverCannotAcceptRideWithoutAssignedDriver() throws Exception {
        when(rideService.getRide(5L)).thenReturn(ride(13L, RideStatus.REQUESTED));

        mockMvc.perform(patch("/api/rides/5/accept")
                        .header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(driverServiceClient);
        verify(rideService, never()).acceptRide(5L);
    }

    @Test
    void missingDriverLookupDeniesLifecycleAction() throws Exception {
        when(rideService.getRide(5L)).thenReturn(assignedRide(RideStatus.ACCEPTED));

        mockMvc.perform(patch("/api/rides/5/start")
                        .header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isForbidden());

        verify(rideService, never()).startRide(5L);
    }

    @Test
    void failedDriverLookupDeniesLifecycleAction() throws Exception {
        when(rideService.getRide(5L)).thenReturn(assignedRide(RideStatus.IN_PROGRESS));
        when(driverServiceClient.getDriver(42L)).thenThrow(new ResourceAccessException("unavailable"));

        mockMvc.perform(patch("/api/rides/5/complete")
                        .header("Authorization", bearer(12L, "DRIVER"))
                        .contentType("application/json").content("{\"distanceKm\":5.0}"))
                .andExpect(status().isForbidden());

        verify(rideService, never()).completeRide(eq(5L), any());
    }

    private static Ride ride(Long passengerId, RideStatus status) {
        Ride ride = new Ride();
        ride.setPassengerId(passengerId);
        ride.setStatus(status);
        ride.setPickupLocation("Colombo");
        ride.setDestination("Kandy");
        return ride;
    }

    private static Ride assignedRide(RideStatus status) {
        Ride ride = ride(13L, status);
        ride.setDriverId(42L);
        return ride;
    }

    private static String createBody(Long passengerId) {
        return "{\"passengerId\":" + passengerId
                + ",\"pickupLocation\":\"Colombo\",\"destination\":\"Kandy\"}";
    }

    private static String bearer(Long accountId, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("RideLink")
                .subject(accountId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        String token = ENCODER.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return "Bearer " + token;
    }
}
