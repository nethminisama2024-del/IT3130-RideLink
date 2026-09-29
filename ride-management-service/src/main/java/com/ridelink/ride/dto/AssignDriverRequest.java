package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotNull;

public class AssignDriverRequest {

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
}