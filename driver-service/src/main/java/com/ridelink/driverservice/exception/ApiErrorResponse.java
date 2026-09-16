package com.ridelink.driverservice.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String code;
    private String message;
    private Map<String, String> fieldErrors;

    public ApiErrorResponse(
            int status,
            String code,
            String message) {

        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public ApiErrorResponse(
            int status,
            String code,
            String message,
            Map<String, String> fieldErrors) {

        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}