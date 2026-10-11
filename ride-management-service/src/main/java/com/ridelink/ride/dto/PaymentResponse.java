package com.ridelink.ride.dto;

public record PaymentResponse(Long paymentId, Long rideId, Long passengerId, Double amount,
                              String paymentMethod, String status, String transactionReference,
                              String createdAt) {
}
