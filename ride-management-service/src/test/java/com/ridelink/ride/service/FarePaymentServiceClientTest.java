package com.ridelink.ride.service;

import com.ridelink.ride.dto.FinalFareRequest;
import com.ridelink.ride.dto.PaymentCreationRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FarePaymentServiceClientTest {
    private static final String BASE_URL = "http://localhost:8084";
    private MockRestServiceServer server;
    private FarePaymentServiceClient client;
    private String serviceKey;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        serviceKey = UUID.randomUUID().toString();
        client = new FarePaymentServiceClient(BASE_URL, serviceKey, builder);
    }

    @Test
    void finalFareCreationSendsKeyAndBody() {
        server.expect(once(), requestTo(BASE_URL + "/api/fares/final"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Ride-Fare-Service-Key", serviceKey))
                .andExpect(content().json("{\"rideId\":7,\"distanceKm\":5.0,"
                        + "\"pickup\":\"Fort\",\"destination\":\"Park\"}"))
                .andRespond(withSuccess());

        client.createFinalFare(new FinalFareRequest(7L, 5.0, "Fort", "Park"));
        server.verify();
    }

    @Test
    void fareLookupSendsKey() {
        server.expect(once(), requestTo(BASE_URL + "/api/fares/ride/7"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Ride-Fare-Service-Key", serviceKey))
                .andRespond(withSuccess("{\"fareAmount\":550.0}", MediaType.APPLICATION_JSON));

        assertEquals(550.0, client.getFinalFare(7L).fareAmount());
        server.verify();
    }

    @Test
    void paymentsByRideLookupSendsKey() {
        server.expect(once(), requestTo(BASE_URL + "/api/payments/ride/7"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Ride-Fare-Service-Key", serviceKey))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(0, client.getPaymentsByRideId(7L).size());
        server.verify();
    }

    @Test
    void paymentCreationSendsKeyAndBody() {
        server.expect(once(), requestTo(BASE_URL + "/api/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Ride-Fare-Service-Key", serviceKey))
                .andExpect(content().json("{\"rideId\":7,\"passengerId\":12,\"amount\":550.0,"
                        + "\"paymentMethod\":\"SIMULATED_CARD\",\"simulateFailure\":false}"))
                .andRespond(withSuccess("{\"paymentId\":3,\"rideId\":7,\"passengerId\":12,"
                        + "\"amount\":550.0,\"status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        assertEquals(3L, client.createPayment(new PaymentCreationRequest(
                7L, 12L, 550.0, "SIMULATED_CARD", false)).paymentId());
        server.verify();
    }
}
