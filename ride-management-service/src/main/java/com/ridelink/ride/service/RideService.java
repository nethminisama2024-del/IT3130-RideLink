package com.ridelink.ride.service;

import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.FinalFareRequest;
import com.ridelink.ride.dto.FareLookupResponse;
import com.ridelink.ride.dto.PaymentCreationRequest;
import com.ridelink.ride.dto.PaymentResponse;
import com.ridelink.ride.dto.PaymentRideRequest;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.exception.InvalidRideStatusException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;
    private final FarePaymentServiceClient farePaymentServiceClient;
    private final AccountServiceClient accountServiceClient;

    public RideService(RideRepository rideRepository, DriverServiceClient driverServiceClient,
                       FarePaymentServiceClient farePaymentServiceClient,
                       AccountServiceClient accountServiceClient) {
        this.rideRepository = rideRepository;
        this.driverServiceClient = driverServiceClient;
        this.farePaymentServiceClient = farePaymentServiceClient;
        this.accountServiceClient = accountServiceClient;
    }

    // Create a new ride
    public Ride createRide(CreateRideRequest request) {

        accountServiceClient.validatePassenger(request.getPassengerId());

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

        List<AvailableDriverResponse> availableDrivers =
                driverServiceClient.findAvailableDrivers(request.getServiceArea());

        if (availableDrivers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No available drivers in service area: " + request.getServiceArea());
        }

        Long driverId = availableDrivers.get(0).id();
        if (driverId == null) {
            throw new IllegalStateException("Driver Service returned a driver without an ID");
        }

        driverServiceClient.markOnRide(driverId);
        ride.setDriverId(driverId);
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
    public Ride completeRide(Long rideId, CompleteRideRequest request) {

        Ride ride = getRide(rideId);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStatusException(
                    "Ride can only be completed when status is IN_PROGRESS"
            );
        }

        farePaymentServiceClient.createFinalFare(new FinalFareRequest(
                rideId,
                request.getDistanceKm(),
                ride.getPickupLocation(),
                ride.getDestination()
        ));

        if (ride.getDriverId() != null) {
            driverServiceClient.markAvailable(ride.getDriverId());
        }

        ride.setStatus(RideStatus.COMPLETED);

        return rideRepository.save(ride);
    }

    public PaymentResponse createPayment(Long rideId, PaymentRideRequest request) {
        Ride ride = getRide(rideId);
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new InvalidRideStatusException(
                    "Payment can only be created when ride status is COMPLETED");
        }

        FareLookupResponse fare = farePaymentServiceClient.getFinalFare(rideId);
        boolean alreadyPaid = farePaymentServiceClient.getPaymentsByRideId(rideId).stream()
                .anyMatch(payment -> "SUCCESS".equals(payment.status()));
        if (alreadyPaid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A successful payment already exists for ride ID: " + rideId);
        }

        return farePaymentServiceClient.createPayment(new PaymentCreationRequest(
                rideId,
                ride.getPassengerId(),
                fare.fareAmount(),
                request.getPaymentMethod(),
                request.isSimulateFailure()
        ));
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

        if (ride.getDriverId() != null) {
            driverServiceClient.markAvailable(ride.getDriverId());
        }

        ride.setStatus(RideStatus.CANCELLED);

        return rideRepository.save(ride);
    }
}
