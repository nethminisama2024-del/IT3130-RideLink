package com.ridelink.driverservice.exception;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(Long driverId) {
        super("Vehicle not found for driver id: " + driverId);
    }
}