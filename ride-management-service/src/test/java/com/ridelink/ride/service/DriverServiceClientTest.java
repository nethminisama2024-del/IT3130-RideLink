package com.ridelink.ride.service;

import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.DriverLookupResponse;
import java.util.List;
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

class DriverServiceClientTest {
    private static final String BASE_URL = "http://localhost:8082";
    private MockRestServiceServer server;
    private DriverServiceClient client;
    private String serviceKey;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        serviceKey = UUID.randomUUID().toString();
        client = new DriverServiceClient(BASE_URL, serviceKey, builder);
    }

    @Test
    void availableDriverSearchIncludesServiceKey() {
        server.expect(once(), requestTo(BASE_URL + "/api/drivers/available?serviceArea=Colombo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Ride-Service-Key", serviceKey))
                .andRespond(withSuccess("[{\"id\":42}]", MediaType.APPLICATION_JSON));

        List<AvailableDriverResponse> drivers = client.findAvailableDrivers("Colombo");

        assertEquals(42L, drivers.get(0).id());
        server.verify();
    }

    @Test
    void driverLookupIncludesServiceKey() {
        server.expect(once(), requestTo(BASE_URL + "/api/drivers/42"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Ride-Service-Key", serviceKey))
                .andRespond(withSuccess("{\"id\":42,\"accountId\":12}", MediaType.APPLICATION_JSON));

        DriverLookupResponse driver = client.getDriver(42L);

        assertEquals(12L, driver.accountId());
        server.verify();
    }

    @Test
    void onRidePatchIncludesServiceKeyAndExistingBody() {
        server.expect(once(), requestTo(BASE_URL + "/api/drivers/42/availability"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Ride-Service-Key", serviceKey))
                .andExpect(content().json("{\"availability\":\"ON_RIDE\"}"))
                .andRespond(withSuccess());

        client.markOnRide(42L);

        server.verify();
    }

    @Test
    void availablePatchIncludesServiceKeyAndExistingBody() {
        server.expect(once(), requestTo(BASE_URL + "/api/drivers/42/availability"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Ride-Service-Key", serviceKey))
                .andExpect(content().json("{\"availability\":\"AVAILABLE\"}"))
                .andRespond(withSuccess());

        client.markAvailable(42L);

        server.verify();
    }
}
