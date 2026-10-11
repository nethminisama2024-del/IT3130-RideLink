package com.ridelink.ride.dto;

public record FinalFareRequest(Long rideId, Double distanceKm, String pickup, String destination) {
}
