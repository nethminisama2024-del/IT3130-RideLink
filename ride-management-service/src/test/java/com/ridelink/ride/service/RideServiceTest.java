package com.ridelink.ride.service;

import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.exception.InvalidRideStatusException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.repository.RideRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private RideService rideService;

    // Test creating a ride
    @Test
    void createRideShouldCreateRequestedRide() {

        CreateRideRequest request = new CreateRideRequest();

        request.setPassengerId(1L);
        request.setPickupLocation("Kandy");
        request.setDestination("Colombo");

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result = rideService.createRide(request);

        assertNotNull(result);
        assertEquals(1L, result.getPassengerId());
        assertEquals("Kandy", result.getPickupLocation());
        assertEquals("Colombo", result.getDestination());
        assertEquals(RideStatus.REQUESTED, result.getStatus());

        verify(rideRepository, times(1))
                .save(any(Ride.class));
    }

    // Test getting an existing ride
    @Test
    void getRideShouldReturnRide() {

        Ride ride = new Ride();
        ride.setPassengerId(1L);
        ride.setPickupLocation("Kandy");
        ride.setDestination("Colombo");
        ride.setStatus(RideStatus.REQUESTED);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        Ride result = rideService.getRide(1L);

        assertNotNull(result);
        assertEquals(RideStatus.REQUESTED, result.getStatus());

        verify(rideRepository, times(1))
                .findById(1L);
    }

    // Test ride not found
    @Test
    void getRideShouldThrowExceptionWhenRideNotFound() {

        when(rideRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                RideNotFoundException.class,
                () -> rideService.getRide(999L)
        );

        verify(rideRepository, times(1))
                .findById(999L);
    }

    // Test passenger ride retrieval
    @Test
    void getPassengerRidesShouldReturnPassengerRides() {

        Ride ride = new Ride();
        ride.setPassengerId(1L);
        ride.setStatus(RideStatus.REQUESTED);

        when(rideRepository.findByPassengerId(1L))
                .thenReturn(List.of(ride));

        List<Ride> rides =
                rideService.getPassengerRides(1L);

        assertEquals(1, rides.size());
        assertEquals(1L, rides.get(0).getPassengerId());

        verify(rideRepository, times(1))
                .findByPassengerId(1L);
    }

    // Test assigning a driver
    @Test
    void assignDriverShouldChangeStatusToAssigned() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.REQUESTED);

        AssignDriverRequest request =
                new AssignDriverRequest();

        request.setDriverId(101L);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result =
                rideService.assignDriver(1L, request);

        assertEquals(101L, result.getDriverId());
        assertEquals(
                RideStatus.ASSIGNED,
                result.getStatus()
        );
    }

    // Test invalid driver assignment
    @Test
    void assignDriverShouldFailWhenRideIsNotRequested() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);

        AssignDriverRequest request =
                new AssignDriverRequest();

        request.setDriverId(101L);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.assignDriver(1L, request)
        );

        verify(rideRepository, never())
                .save(any(Ride.class));
    }

    // Test accepting a ride
    @Test
    void acceptRideShouldChangeStatusToAccepted() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.ASSIGNED);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result =
                rideService.acceptRide(1L);

        assertEquals(
                RideStatus.ACCEPTED,
                result.getStatus()
        );
    }

    // Test starting a ride
    @Test
    void startRideShouldChangeStatusToInProgress() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.ACCEPTED);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result =
                rideService.startRide(1L);

        assertEquals(
                RideStatus.IN_PROGRESS,
                result.getStatus()
        );
    }

    // Test completing a ride
    @Test
    void completeRideShouldChangeStatusToCompleted() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result =
                rideService.completeRide(1L);

        assertEquals(
                RideStatus.COMPLETED,
                result.getStatus()
        );
    }

    // Test cancelling a ride
    @Test
    void cancelRideShouldChangeStatusToCancelled() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.REQUESTED);

        when(rideRepository.findById(2L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result =
                rideService.cancelRide(2L);

        assertEquals(
                RideStatus.CANCELLED,
                result.getStatus()
        );
    }

    // Test invalid cancellation
    @Test
    void cancelRideShouldFailForCompletedRide() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.cancelRide(1L)
        );

        verify(rideRepository, never())
                .save(any(Ride.class));
    }
}