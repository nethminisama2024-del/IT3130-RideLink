package com.ridelink.driverservice.dto;

import com.ridelink.driverservice.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public class AvailabilityRequest {

    @NotNull(message = "Availability status is required")
    private AvailabilityStatus availability;

    public AvailabilityStatus getAvailability() {
        return availability;
    }

    public void setAvailability(AvailabilityStatus availability) {
        this.availability = availability;
    }
}