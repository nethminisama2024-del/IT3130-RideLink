package com.ridelink.driverservice.exception;

public class NoEligibleDriverException extends RuntimeException {

    public NoEligibleDriverException(String serviceArea) {
        super("No eligible drivers available in service area: " + serviceArea);
    }
}