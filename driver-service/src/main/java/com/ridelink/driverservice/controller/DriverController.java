package com.ridelink.driverservice.controller;

import com.ridelink.driverservice.dto.AvailabilityRequest;
import com.ridelink.driverservice.dto.DriverRequest;
import com.ridelink.driverservice.dto.DriverUpdateRequest;
import com.ridelink.driverservice.dto.LocationRequest;
import com.ridelink.driverservice.entity.Driver;
import com.ridelink.driverservice.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public ResponseEntity<Driver> createDriver(
            @Valid @RequestBody DriverRequest request) {

        Driver driver = driverService.createDriver(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(driver);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Driver> getDriver(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                driverService.getDriver(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Driver> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverUpdateRequest request) {

        return ResponseEntity.ok(
                driverService.updateDriver(id, request)
        );
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<Driver> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest request) {

        return ResponseEntity.ok(
                driverService.updateAvailability(id, request)
        );
    }

    @PatchMapping("/{id}/location")
    public ResponseEntity<Driver> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequest request) {

        return ResponseEntity.ok(
                driverService.updateLocation(id, request)
        );
    }

    @GetMapping("/available")
    public ResponseEntity<List<Driver>> getEligibleDrivers(
            @RequestParam String serviceArea) {

        return ResponseEntity.ok(
                driverService.getEligibleDrivers(serviceArea)
        );
    }
}