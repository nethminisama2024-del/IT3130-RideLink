package com.ridelink.ride.exception;

public class InvalidRideStatusException extends RuntimeException {

    public InvalidRideStatusException(String message) {
        super(message);
    }
}