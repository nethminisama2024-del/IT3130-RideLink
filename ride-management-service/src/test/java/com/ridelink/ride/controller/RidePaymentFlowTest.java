package com.ridelink.ride.controller;

import com.ridelink.ride.dto.FareLookupResponse;
import com.ridelink.ride.dto.PaymentCreationRequest;
import com.ridelink.ride.dto.PaymentResponse;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.service.AccountServiceClient;
import com.ridelink.ride.service.DriverServiceClient;
import com.ridelink.ride.service.FarePaymentServiceClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RidePaymentFlowTest {
    private static final String SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final JwtEncoder ENCODER = NimbusJwtEncoder.withSecretKey(
            new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> SECRET);
        registry.add("driver.service.key", () -> UUID.randomUUID().toString());
        registry.add("ride.fare.service.key", () -> UUID.randomUUID().toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:ride_payment_flow_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean RideRepository rideRepository;
    @MockitoBean DriverServiceClient driverServiceClient;
    @MockitoBean AccountServiceClient accountServiceClient;
    @MockitoBean FarePaymentServiceClient farePaymentServiceClient;

    @Test
    void ownerCanPayCompletedRideThroughAllThreeFarePaymentCalls() throws Exception {
        when(rideRepository.findById(9L)).thenReturn(Optional.of(completedRide()));
        when(farePaymentServiceClient.getFinalFare(9L)).thenReturn(new FareLookupResponse(550.0));
        when(farePaymentServiceClient.getPaymentsByRideId(9L)).thenReturn(List.of());
        PaymentCreationRequest expected = new PaymentCreationRequest(
                9L, 12L, 550.0, "SIMULATED_CARD", false);
        when(farePaymentServiceClient.createPayment(expected)).thenReturn(
                new PaymentResponse(3L, 9L, 12L, 550.0, "SIMULATED_CARD", "SUCCESS", "ref", "now"));

        mockMvc.perform(post("/api/rides/9/payment")
                        .header("Authorization", bearer(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").value(9))
                .andExpect(jsonPath("$.passengerId").value(12));

        var order = inOrder(farePaymentServiceClient);
        order.verify(farePaymentServiceClient).getFinalFare(9L);
        order.verify(farePaymentServiceClient).getPaymentsByRideId(9L);
        order.verify(farePaymentServiceClient).createPayment(expected);
    }

    @Test
    void successfulExistingPaymentRemainsConflict() throws Exception {
        when(rideRepository.findById(9L)).thenReturn(Optional.of(completedRide()));
        when(farePaymentServiceClient.getFinalFare(9L)).thenReturn(new FareLookupResponse(550.0));
        when(farePaymentServiceClient.getPaymentsByRideId(9L)).thenReturn(List.of(
                new PaymentResponse(3L, 9L, 12L, 550.0, "SIMULATED_CARD", "SUCCESS", "ref", "now")));

        mockMvc.perform(post("/api/rides/9/payment")
                        .header("Authorization", bearer(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        verify(farePaymentServiceClient, never()).createPayment(any(PaymentCreationRequest.class));
    }

    private static Ride completedRide() {
        Ride ride = new Ride();
        ride.setPassengerId(12L);
        ride.setStatus(RideStatus.COMPLETED);
        ride.setPickupLocation("Fort");
        ride.setDestination("Park");
        return ride;
    }

    private static String bearer(Long accountId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("RideLink")
                .subject(accountId.toString())
                .claim("role", "PASSENGER")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        String token = ENCODER.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return "Bearer " + token;
    }
}
