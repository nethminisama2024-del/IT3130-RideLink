package com.ridelink.driverservice.exception;

public class DuplicateVehicleRegistrationException extends RuntimeException {

    public DuplicateVehicleRegistrationException(String registrationNumber) {
        super("Vehicle registration number already exists: " + registrationNumber);
    }
}