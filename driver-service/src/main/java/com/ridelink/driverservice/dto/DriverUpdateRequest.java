package com.ridelink.driverservice.dto;

import com.ridelink.driverservice.enums.OperationalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DriverUpdateRequest {

    @NotBlank(message = "Service area is required")
    private String serviceArea;

    @NotNull(message = "Operational status is required")
    private OperationalStatus operationalStatus;

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public OperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(OperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }
}