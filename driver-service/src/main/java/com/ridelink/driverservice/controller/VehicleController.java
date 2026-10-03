package com.ridelink.driverservice.controller;

import com.ridelink.driverservice.dto.VehicleRequest;
import com.ridelink.driverservice.entity.Vehicle;
import com.ridelink.driverservice.security.DriverAccessService;
import com.ridelink.driverservice.service.VehicleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers/{driverId}/vehicle")
@Tag(
        name = "Vehicle Management",
        description = "APIs for adding, retrieving and updating driver vehicle details"
)
public class VehicleController {

    private final VehicleService vehicleService;
    private final DriverAccessService driverAccessService;

    public VehicleController(VehicleService vehicleService, DriverAccessService driverAccessService) {
        this.vehicleService = vehicleService;
        this.driverAccessService = driverAccessService;
    }

    @PostMapping
    @Operation(
            summary = "Add vehicle",
            description = "Registers a vehicle for the specified driver"
    )
    public ResponseEntity<Vehicle> addVehicle(
            @PathVariable Long driverId,
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication) {

        driverAccessService.requireOwnerOrAdmin(driverId, authentication);
        Vehicle vehicle =
                vehicleService.addVehicle(driverId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(vehicle);
    }

    @GetMapping
    @Operation(
            summary = "Get driver vehicle",
            description = "Retrieves the vehicle registered to the specified driver"
    )
    public ResponseEntity<Vehicle> getVehicle(
            @PathVariable Long driverId, Authentication authentication) {

        driverAccessService.requireOwnerOrAdmin(driverId, authentication);
        return ResponseEntity.ok(
                vehicleService.getVehicleByDriver(driverId)
        );
    }

    @PutMapping
    @Operation(
            summary = "Update vehicle",
            description = "Updates the vehicle details for the specified driver"
    )
    public ResponseEntity<Vehicle> updateVehicle(
            @PathVariable Long driverId,
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication) {

        driverAccessService.requireOwnerOrAdmin(driverId, authentication);
        return ResponseEntity.ok(
                vehicleService.updateVehicle(driverId, request)
        );
    }
}
