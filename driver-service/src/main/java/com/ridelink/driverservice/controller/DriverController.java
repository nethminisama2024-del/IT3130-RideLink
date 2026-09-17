package com.ridelink.driverservice.controller;

import com.ridelink.driverservice.dto.AvailabilityRequest;
import com.ridelink.driverservice.dto.DriverRequest;
import com.ridelink.driverservice.dto.DriverUpdateRequest;
import com.ridelink.driverservice.dto.LocationRequest;
import com.ridelink.driverservice.entity.Driver;
import com.ridelink.driverservice.service.DriverService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(
        name = "Driver Management",
        description = "APIs for driver operational profiles, availability, location and eligible driver search"
)
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @Operation(
            summary = "Create driver profile",
            description = "Creates a new operational driver profile"
    )
    public ResponseEntity<Driver> createDriver(
            @Valid @RequestBody DriverRequest request) {

        Driver driver = driverService.createDriver(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(driver);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get driver",
            description = "Retrieves a driver by driver ID"
    )
    public ResponseEntity<Driver> getDriver(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                driverService.getDriver(id)
        );
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update driver",
            description = "Updates driver operational details such as service area and operational status"
    )
    public ResponseEntity<Driver> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverUpdateRequest request) {

        return ResponseEntity.ok(
                driverService.updateDriver(id, request)
        );
    }

    @PatchMapping("/{id}/availability")
    @Operation(
            summary = "Update driver availability",
            description = "Changes driver availability to AVAILABLE, UNAVAILABLE or ON_RIDE"
    )
    public ResponseEntity<Driver> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest request) {

        return ResponseEntity.ok(
                driverService.updateAvailability(id, request)
        );
    }

    @PatchMapping("/{id}/location")
    @Operation(
            summary = "Update simulated driver location",
            description = "Updates the driver's simulated latitude and longitude"
    )
    public ResponseEntity<Driver> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequest request) {

        return ResponseEntity.ok(
                driverService.updateLocation(id, request)
        );
    }

    @GetMapping("/available")
    @Operation(
            summary = "Find eligible available drivers",
            description = "Returns active and available drivers matching the requested service area"
    )
    public ResponseEntity<List<Driver>> getEligibleDrivers(
            @RequestParam String serviceArea) {

        return ResponseEntity.ok(
                driverService.getEligibleDrivers(serviceArea)
        );
    }
}