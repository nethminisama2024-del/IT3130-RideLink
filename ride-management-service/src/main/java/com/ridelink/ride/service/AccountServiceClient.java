package com.ridelink.ride.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccountServiceClient {

    private final RestClient restClient;

    public AccountServiceClient(@Value("${account.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validatePassenger(Long accountId) {
        ResponseEntity<Void> response;
        try {
            response = restClient.get()
                    .uri("/api/accounts/{accountId}/passenger-validation", accountId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Passenger account is invalid: " + accountId, exception);
        }

        if (response.getStatusCode().value() != HttpStatus.NO_CONTENT.value()) {
            throw new IllegalStateException(
                    "Unexpected Account Service response: " + response.getStatusCode());
        }
    }
}
