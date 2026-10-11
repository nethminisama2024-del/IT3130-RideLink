package com.ridelink.ride.service;

import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.DriverAvailabilityRequest;
import com.ridelink.ride.dto.DriverLookupResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class DriverServiceClient {
    private static final String RIDE_SERVICE_KEY_HEADER = "X-Ride-Service-Key";

    private final RestClient restClient;

    @Autowired
    public DriverServiceClient(@Value("${driver.service.base-url}") String baseUrl,
                               @Value("${driver.service.key}") String serviceKey) {
        this(baseUrl, serviceKey, RestClient.builder());
    }

    DriverServiceClient(String baseUrl, String serviceKey, RestClient.Builder builder) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalArgumentException("RIDE_SERVICE_KEY must not be blank");
        }
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader(RIDE_SERVICE_KEY_HEADER, serviceKey)
                .build();
    }

    public List<AvailableDriverResponse> findAvailableDrivers(String serviceArea) {
        try {
            AvailableDriverResponse[] drivers = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/drivers/available")
                            .queryParam("serviceArea", serviceArea).build())
                    .retrieve()
                    .body(AvailableDriverResponse[].class);
            return drivers == null ? List.of() : Arrays.asList(drivers);
        } catch (HttpClientErrorException.NotFound exception) {
            return List.of();
        }
    }

    public DriverLookupResponse getDriver(Long driverId) {
        return restClient.get()
                .uri("/api/drivers/{driverId}", driverId)
                .retrieve()
                .body(DriverLookupResponse.class);
    }

    public void markOnRide(Long driverId) {
        updateAvailability(driverId, "ON_RIDE");
    }

    public void markAvailable(Long driverId) {
        updateAvailability(driverId, "AVAILABLE");
    }

    private void updateAvailability(Long driverId, String availability) {
        restClient.patch()
                .uri("/api/drivers/{driverId}/availability", driverId)
                .body(new DriverAvailabilityRequest(availability))
                .retrieve()
                .toBodilessEntity();
    }
}
