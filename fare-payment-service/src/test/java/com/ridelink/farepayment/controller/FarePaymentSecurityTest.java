package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.service.FareService;
import com.ridelink.farepayment.service.PaymentService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FarePaymentSecurityTest {
    private static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final String SERVICE_KEY = UUID.randomUUID().toString();
    private static final JwtEncoder ENCODER = new NimbusJwtEncoder(
            new ImmutableSecret<>(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("ride.fare.service.key", () -> SERVICE_KEY);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:fare_security_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
    }

    @Autowired MockMvc mockMvc;
    @MockBean FareService fareService;
    @MockBean PaymentService paymentService;

    @Test
    void noAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/payments/3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalKeyCanCreateFinalFare() throws Exception {
        when(fareService.calculateFinalFare(any())).thenReturn(fare());

        mockMvc.perform(post("/api/fares/final")
                        .header("X-Ride-Fare-Service-Key", SERVICE_KEY)
                        .contentType("application/json")
                        .content("{\"rideId\":7,\"distanceKm\":5.0,"
                                + "\"pickup\":\"Fort\",\"destination\":\"Park\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(7));
    }

    @Test
    void internalKeyCanReadFareByRide() throws Exception {
        when(fareService.getFareByRideId(7L)).thenReturn(fare());

        mockMvc.perform(get("/api/fares/ride/7")
                        .header("X-Ride-Fare-Service-Key", SERVICE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fareAmount").value(550.0));
    }

    @Test
    void internalKeyCanCreatePayment() throws Exception {
        when(paymentService.processPayment(any())).thenReturn(payment());

        mockMvc.perform(post("/api/payments")
                        .header("X-Ride-Fare-Service-Key", SERVICE_KEY)
                        .contentType("application/json")
                        .content("{\"rideId\":7,\"passengerId\":12,\"amount\":550.0,"
                                + "\"paymentMethod\":\"SIMULATED_CARD\",\"simulateFailure\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(3));
    }

    @Test
    void internalKeyCanReadPaymentsByRideForDuplicateCheck() throws Exception {
        when(paymentService.getPaymentsByRideId(7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/ride/7")
                        .header("X-Ride-Fare-Service-Key", SERVICE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void passengerCannotCreateFinalFare() throws Exception {
        mockMvc.perform(post("/api/fares/final")
                        .header("Authorization", bearer(12L, "PASSENGER"))
                        .contentType("application/json")
                        .content("{\"rideId\":7,\"distanceKm\":5.0}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(fareService);
    }

    @Test
    void passengerCannotCreatePayment() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(12L, "PASSENGER"))
                        .contentType("application/json")
                        .content("{\"rideId\":7,\"amount\":550.0}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(paymentService);
    }

    @Test
    void owningPassengerCanReadPayment() throws Exception {
        when(paymentService.getPaymentById(3L)).thenReturn(payment());

        mockMvc.perform(get("/api/payments/3")
                        .header("Authorization", bearer(12L, "PASSENGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengerId").value(12));
    }

    @Test
    void differentPassengerCannotReadPayment() throws Exception {
        when(paymentService.getPaymentById(3L)).thenReturn(payment());

        mockMvc.perform(get("/api/payments/3")
                        .header("Authorization", bearer(99L, "PASSENGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadPayment() throws Exception {
        when(paymentService.getPaymentById(3L)).thenReturn(payment());

        mockMvc.perform(get("/api/payments/3")
                        .header("Authorization", bearer(99L, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void receiptUsesSamePassengerOwnershipRule() throws Exception {
        when(paymentService.getReceipt(3L)).thenReturn(new ReceiptResponse(
                3L, 7L, 12L, 550.0, "SIMULATED_CARD", "SUCCESS", LocalDateTime.now()));

        mockMvc.perform(get("/api/payments/3/receipt")
                        .header("Authorization", bearer(99L, "PASSENGER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/payments/3/receipt")
                        .header("Authorization", bearer(12L, "PASSENGER")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidInternalKeyCannotReadFare() throws Exception {
        mockMvc.perform(get("/api/fares/ride/7")
                        .header("X-Ride-Fare-Service-Key", "invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(fareService);
    }

    private static FareRecord fare() {
        FareRecord record = new FareRecord();
        record.setId(2L);
        record.setRideId(7L);
        record.setDistanceKm(5.0);
        record.setFareAmount(550.0);
        record.setPickupLocation("Fort");
        record.setDestination("Park");
        return record;
    }

    private static PaymentResponse payment() {
        return new PaymentResponse(3L, 7L, 12L, 550.0, "SIMULATED_CARD", "SUCCESS",
                "TXN-test", LocalDateTime.now());
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
        return "Bearer " + ENCODER.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
