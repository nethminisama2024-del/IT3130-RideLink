package com.ridelink.driverservice.exception;

public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(Long driverId) {
        super("Driver not found with id: " + driverId);
    }
}