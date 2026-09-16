package com.ridelink.driverservice.controller;

import com.ridelink.driverservice.dto.VehicleRequest;
import com.ridelink.driverservice.entity.Vehicle;
import com.ridelink.driverservice.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers/{driverId}/vehicle")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<Vehicle> addVehicle(
            @PathVariable Long driverId,
            @Valid @RequestBody VehicleRequest request) {

        Vehicle vehicle =
                vehicleService.addVehicle(driverId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(vehicle);
    }

    @GetMapping
    public ResponseEntity<Vehicle> getVehicle(
            @PathVariable Long driverId) {

        return ResponseEntity.ok(
                vehicleService.getVehicleByDriver(driverId)
        );
    }

    @PutMapping
    public ResponseEntity<Vehicle> updateVehicle(
            @PathVariable Long driverId,
            @Valid @RequestBody VehicleRequest request) {

        return ResponseEntity.ok(
                vehicleService.updateVehicle(driverId, request)
        );
    }
}