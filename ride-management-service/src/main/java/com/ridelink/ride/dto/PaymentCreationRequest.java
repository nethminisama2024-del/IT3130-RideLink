package com.ridelink.ride.dto;

public record PaymentCreationRequest(Long rideId, Long passengerId, Double amount,
                                     String paymentMethod, boolean simulateFailure) {
}
