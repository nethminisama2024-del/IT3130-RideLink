package com.ridelink.ride.service;

import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.exception.InvalidRideStatusException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RideService {

    private final RideRepository rideRepository;

    public RideService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    // Create a new ride
    public Ride createRide(CreateRideRequest request) {

        Ride ride = new Ride();

        ride.setPassengerId(request.getPassengerId());
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDestination(request.getDestination());
        ride.setStatus(RideStatus.REQUESTED);

        return rideRepository.save(ride);
    }

    // Get one ride
    public Ride getRide(Long id) {

        return rideRepository.findById(id)
                .orElseThrow(() ->
                        new RideNotFoundException(
                                "Ride with ID " + id + " was not found"
                        )
                );
    }

    // Get passenger rides
    public List<Ride> getPassengerRides(Long passengerId) {

        return rideRepository.findByPassengerId(passengerId);
    }

    // Assign driver
    public Ride assignDriver(
            Long rideId,
            AssignDriverRequest request) {

        Ride ride = getRide(rideId);

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStatusException(
                    "Driver can only be assigned when ride status is REQUESTED"
            );
        }

        ride.setDriverId(request.getDriverId());
        ride.setStatus(RideStatus.ASSIGNED);

        return rideRepository.save(ride);
    }

    // Accept ride
    public Ride acceptRide(Long rideId) {

        Ride ride = getRide(rideId);

        if (ride.getStatus() != RideStatus.ASSIGNED) {
            throw new InvalidRideStatusException(
                    "Ride can only be accepted when status is ASSIGNED"
            );
        }

        ride.setStatus(RideStatus.ACCEPTED);

        return rideRepository.save(ride);
    }

    // Start ride
    public Ride startRide(Long rideId) {

        Ride ride = getRide(rideId);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideStatusException(
                    "Ride can only be started when status is ACCEPTED"
            );
        }

        ride.setStatus(RideStatus.IN_PROGRESS);

        return rideRepository.save(ride);
    }

    // Complete ride
    public Ride completeRide(Long rideId) {

        Ride ride = getRide(rideId);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStatusException(
                    "Ride can only be completed when status is IN_PROGRESS"
            );
        }

        ride.setStatus(RideStatus.COMPLETED);

        return rideRepository.save(ride);
    }

    // Cancel ride
    public Ride cancelRide(Long rideId) {

        Ride ride = getRide(rideId);

        if (ride.getStatus() == RideStatus.IN_PROGRESS ||
                ride.getStatus() == RideStatus.COMPLETED ||
                ride.getStatus() == RideStatus.CANCELLED) {

            throw new InvalidRideStatusException(
                    "Ride cannot be cancelled when status is "
                            + ride.getStatus()
            );
        }

        ride.setStatus(RideStatus.CANCELLED);

        return rideRepository.save(ride);
    }
}