package com.ridelink.farepayment.dto;

public class FareEstimateResponse {
    private Double distanceKm;
    private Double estimatedFare;
    private String pickup;
    private String destination;

    public FareEstimateResponse(Double distanceKm, Double estimatedFare,
                                String pickup, String destination) {
        this.distanceKm = distanceKm;
        this.estimatedFare = estimatedFare;
        this.pickup = pickup;
        this.destination = destination;
    }

    public Double getDistanceKm() { return distanceKm; }
    public Double getEstimatedFare() { return estimatedFare; }
    public String getPickup() { return pickup; }
    public String getDestination() { return destination; }
}