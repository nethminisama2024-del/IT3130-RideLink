package com.ridelink.driverservice.controller;

import com.ridelink.driverservice.entity.Driver;
import com.ridelink.driverservice.enums.AvailabilityStatus;
import com.ridelink.driverservice.service.DriverService;
import com.ridelink.driverservice.service.VehicleService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DriverSecurityTest {
    private static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final String RIDE_SERVICE_KEY = UUID.randomUUID().toString();
    private static final JwtEncoder ENCODER = NimbusJwtEncoder.withSecretKey(
            new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("ride.service.key", () -> RIDE_SERVICE_KEY);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:driver_security_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean DriverService driverService;
    @MockitoBean VehicleService vehicleService;

    @Test
    void noTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/drivers/42"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanReadDriver() throws Exception {
        when(driverService.getDriver(42L)).thenReturn(driver(42L, 12L, AvailabilityStatus.AVAILABLE));

        mockMvc.perform(get("/api/drivers/42").header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(12));
    }

    @Test
    void unrelatedDriverCannotReadDriver() throws Exception {
        when(driverService.getDriver(42L)).thenReturn(driver(42L, 99L, AvailabilityStatus.AVAILABLE));

        mockMvc.perform(get("/api/drivers/42").header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateDriver() throws Exception {
        when(driverService.updateDriver(eq(42L), any()))
                .thenReturn(driver(42L, 12L, AvailabilityStatus.AVAILABLE));

        mockMvc.perform(put("/api/drivers/42").header("Authorization", bearer(99L, "ADMIN"))
                        .contentType("application/json")
                        .content("{\"serviceArea\":\"Colombo\",\"operationalStatus\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void driverCannotUpdateOperationalStatus() throws Exception {
        mockMvc.perform(put("/api/drivers/42").header("Authorization", bearer(12L, "DRIVER"))
                        .contentType("application/json")
                        .content("{\"serviceArea\":\"Colombo\",\"operationalStatus\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rideServiceCanFindAvailableDrivers() throws Exception {
        when(driverService.getEligibleDrivers("Colombo"))
                .thenReturn(List.of(driver(42L, 12L, AvailabilityStatus.AVAILABLE)));

        mockMvc.perform(get("/api/drivers/available").param("serviceArea", "Colombo")
                        .header("X-Ride-Service-Key", RIDE_SERVICE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42));
    }

    @Test
    void rideServiceCanReadDriver() throws Exception {
        when(driverService.getDriver(42L)).thenReturn(driver(42L, 12L, AvailabilityStatus.AVAILABLE));

        mockMvc.perform(get("/api/drivers/42").header("X-Ride-Service-Key", RIDE_SERVICE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(12));
    }

    @Test
    void rideServiceCanChangeAvailability() throws Exception {
        when(driverService.updateAvailability(eq(42L), any()))
                .thenReturn(driver(42L, 12L, AvailabilityStatus.ON_RIDE));

        mockMvc.perform(patch("/api/drivers/42/availability")
                        .header("X-Ride-Service-Key", RIDE_SERVICE_KEY)
                        .contentType("application/json").content("{\"availability\":\"ON_RIDE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("ON_RIDE"));
    }

    @Test
    void missingOrInvalidRideKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/drivers/available").param("serviceArea", "Colombo"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/drivers/available").param("serviceArea", "Colombo")
                        .header("X-Ride-Service-Key", "invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(driverService);
    }

    @Test
    void jwtCannotClaimInternalRideServiceRole() throws Exception {
        mockMvc.perform(get("/api/drivers/available").param("serviceArea", "Colombo")
                        .header("Authorization", bearer(12L, "RIDE_SERVICE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(driverService);
    }

    @Test
    void passengerCannotReadDriver() throws Exception {
        mockMvc.perform(get("/api/drivers/42").header("Authorization", bearer(12L, "PASSENGER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(driverService);
    }

    @Test
    void driverCannotReleaseThemselvesFromOnRide() throws Exception {
        when(driverService.getDriver(42L)).thenReturn(driver(42L, 12L, AvailabilityStatus.ON_RIDE));

        mockMvc.perform(patch("/api/drivers/42/availability")
                        .header("Authorization", bearer(12L, "DRIVER"))
                        .contentType("application/json").content("{\"availability\":\"AVAILABLE\"}"))
                .andExpect(status().isForbidden());

        verify(driverService, never()).updateAvailability(eq(42L), any());
    }

    @Test
    void driverCanChangeIdleAvailability() throws Exception {
        when(driverService.getDriver(42L)).thenReturn(driver(42L, 12L, AvailabilityStatus.UNAVAILABLE));
        when(driverService.updateAvailability(eq(42L), any()))
                .thenReturn(driver(42L, 12L, AvailabilityStatus.AVAILABLE));

        mockMvc.perform(patch("/api/drivers/42/availability")
                        .header("Authorization", bearer(12L, "DRIVER"))
                        .contentType("application/json").content("{\"availability\":\"AVAILABLE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void driverCannotCreateProfileForDifferentAccount() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .header("Authorization", bearer(12L, "DRIVER"))
                        .contentType("application/json").content(createBody(99L)))
                .andExpect(status().isForbidden());

        verify(driverService, never()).createDriver(any());
    }

    @Test
    void driverCanCreateOwnProfile() throws Exception {
        when(driverService.createDriver(any()))
                .thenReturn(driver(42L, 12L, AvailabilityStatus.UNAVAILABLE));

        mockMvc.perform(post("/api/drivers")
                        .header("Authorization", bearer(12L, "DRIVER"))
                        .contentType("application/json").content(createBody(12L)))
                .andExpect(status().isCreated());
    }

    @Test
    void ownerCanReadVehicle() throws Exception {
        when(driverService.getDriver(42L)).thenReturn(driver(42L, 12L, AvailabilityStatus.AVAILABLE));

        mockMvc.perform(get("/api/drivers/42/vehicle")
                        .header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isOk());

        verify(vehicleService).getVehicleByDriver(42L);
    }

    @Test
    void unrelatedDriverCannotReadVehicle() throws Exception {
        when(driverService.getDriver(42L)).thenReturn(driver(42L, 99L, AvailabilityStatus.AVAILABLE));

        mockMvc.perform(get("/api/drivers/42/vehicle")
                        .header("Authorization", bearer(12L, "DRIVER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vehicleService);
    }

    private static Driver driver(Long id, Long accountId, AvailabilityStatus availability) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setAccountId(accountId);
        driver.setAvailability(availability);
        driver.setLicenseNumber("B1234567");
        driver.setServiceArea("Colombo");
        return driver;
    }

    private static String createBody(Long accountId) {
        return "{\"accountId\":" + accountId
                + ",\"licenseNumber\":\"B1234567\",\"serviceArea\":\"Colombo\"}";
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
