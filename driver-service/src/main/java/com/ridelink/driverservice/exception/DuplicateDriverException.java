package com.ridelink.driverservice.exception;

public class DuplicateDriverException extends RuntimeException {

    public DuplicateDriverException(String message) {
        super(message);
    }
}