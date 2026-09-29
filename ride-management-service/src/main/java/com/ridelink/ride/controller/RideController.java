package com.ridelink.ride.controller;

import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.service.RideService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@Tag(
        name = "Ride Management",
        description = "APIs for creating rides and managing the ride lifecycle"
)
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    // Create Ride
    @Operation(
            summary = "Create a new ride request",
            description = "Creates a new ride with REQUESTED status"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ride created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ride createRide(
            @Valid @RequestBody CreateRideRequest request) {

        return rideService.createRide(request);
    }

    // Get Ride By ID
    @Operation(
            summary = "Get ride by ID",
            description = "Retrieves a single ride using its ride ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride found"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @GetMapping("/{id}")
    public Ride getRide(@PathVariable Long id) {

        return rideService.getRide(id);
    }

    // Get Passenger Rides
    @Operation(
            summary = "Get rides for a passenger",
            description = "Retrieves all rides belonging to a passenger"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Passenger rides retrieved successfully"
    )
    @GetMapping
    public List<Ride> getPassengerRides(
            @RequestParam Long passengerId) {

        return rideService.getPassengerRides(passengerId);
    }

    // Assign Driver
    @Operation(
            summary = "Assign a driver to a ride",
            description = "Assigns a driver and changes ride status from REQUESTED to ASSIGNED"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride status"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PatchMapping("/{id}/assign")
    public Ride assignDriver(
            @PathVariable Long id,
            @Valid @RequestBody AssignDriverRequest request) {

        return rideService.assignDriver(id, request);
    }

    // Accept Ride
    @Operation(
            summary = "Accept an assigned ride",
            description = "Changes ride status from ASSIGNED to ACCEPTED"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride accepted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride status"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PatchMapping("/{id}/accept")
    public Ride acceptRide(@PathVariable Long id) {

        return rideService.acceptRide(id);
    }

    // Start Ride
    @Operation(
            summary = "Start a ride",
            description = "Changes ride status from ACCEPTED to IN_PROGRESS"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride status"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PatchMapping("/{id}/start")
    public Ride startRide(@PathVariable Long id) {

        return rideService.startRide(id);
    }

    // Complete Ride
    @Operation(
            summary = "Complete a ride",
            description = "Changes ride status from IN_PROGRESS to COMPLETED"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride status"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PatchMapping("/{id}/complete")
    public Ride completeRide(@PathVariable Long id) {

        return rideService.completeRide(id);
    }

    // Cancel Ride
    @Operation(
            summary = "Cancel a ride",
            description = "Cancels a ride when cancellation is allowed"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Ride cannot be cancelled in its current status"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PatchMapping("/{id}/cancel")
    public Ride cancelRide(@PathVariable Long id) {

        return rideService.cancelRide(id);
    }
}