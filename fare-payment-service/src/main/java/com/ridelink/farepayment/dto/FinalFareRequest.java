package com.ridelink.farepayment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class FinalFareRequest {

    @NotNull(message = "Ride ID is required")
    private Long rideId;

    @NotNull(message = "Distance is required")
    @Positive(message = "Distance must be positive")
    private Double distanceKm;

    private String pickup;
    private String destination;

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public String getPickup() { return pickup; }
    public void setPickup(String pickup) { this.pickup = pickup; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}