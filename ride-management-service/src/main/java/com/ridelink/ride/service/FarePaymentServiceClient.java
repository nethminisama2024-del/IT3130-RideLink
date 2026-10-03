package com.ridelink.ride.service;

import com.ridelink.ride.dto.FinalFareRequest;
import com.ridelink.ride.dto.FareLookupResponse;
import com.ridelink.ride.dto.PaymentCreationRequest;
import com.ridelink.ride.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class FarePaymentServiceClient {
    private static final String SERVICE_KEY_HEADER = "X-Ride-Fare-Service-Key";

    private final RestClient restClient;

    @Autowired
    public FarePaymentServiceClient(@Value("${fare-payment.service.base-url}") String baseUrl,
                                    @Value("${ride.fare.service.key}") String serviceKey) {
        this(baseUrl, serviceKey, RestClient.builder());
    }

    FarePaymentServiceClient(String baseUrl, String serviceKey, RestClient.Builder builder) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalArgumentException("RIDE_FARE_SERVICE_KEY must not be blank");
        }
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader(SERVICE_KEY_HEADER, serviceKey)
                .build();
    }

    public void createFinalFare(FinalFareRequest request) {
        restClient.post()
                .uri("/api/fares/final")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public FareLookupResponse getFinalFare(Long rideId) {
        Map<?, ?> fare;
        try {
            fare = call(() -> restClient.get()
                    .uri("/api/fares/ride/{rideId}", rideId)
                    .retrieve()
                    .body(Map.class));
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Final fare not found for ride ID: " + rideId, exception);
        }

        Object amount = fare == null ? null : fare.get("fareAmount");
        if (!(amount instanceof Number number) || number.doubleValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Fare & Payment Service returned an invalid final fare");
        }
        return new FareLookupResponse(number.doubleValue());
    }

    public List<PaymentResponse> getPaymentsByRideId(Long rideId) {
        PaymentResponse[] payments = call(() -> restClient.get()
                .uri("/api/payments/ride/{rideId}", rideId)
                .retrieve()
                .body(PaymentResponse[].class));
        return payments == null ? List.of() : Arrays.asList(payments);
    }

    public PaymentResponse createPayment(PaymentCreationRequest request) {
        PaymentResponse payment = call(() -> restClient.post()
                .uri("/api/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class));
        if (payment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Fare & Payment Service returned no payment");
        }
        return payment;
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException | HttpServerErrorException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Fare & Payment Service is unavailable", exception);
        }
    }
}
