package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignDriverRequest {

    @NotBlank(message = "Service area is required")
    private String serviceArea;

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }
}
