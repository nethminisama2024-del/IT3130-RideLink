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
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverServiceClient driverServiceClient;

    @Mock
    private FarePaymentServiceClient farePaymentServiceClient;

    @Mock
    private AccountServiceClient accountServiceClient;

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

        InOrder order = inOrder(accountServiceClient, rideRepository);
        order.verify(accountServiceClient).validatePassenger(1L);
        order.verify(rideRepository).save(any(Ride.class));
    }

    @Test
    void createRideShouldNotSaveWhenPassengerIsInvalid() {
        CreateRideRequest request = new CreateRideRequest();
        request.setPassengerId(1L);
        request.setPickupLocation("Kandy");
        request.setDestination("Colombo");

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid passenger"))
                .when(accountServiceClient).validatePassenger(1L);

        assertThrows(ResponseStatusException.class, () -> rideService.createRide(request));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void createRideShouldNotSaveWhenAccountServiceIsUnavailable() {
        CreateRideRequest request = new CreateRideRequest();
        request.setPassengerId(1L);
        request.setPickupLocation("Kandy");
        request.setDestination("Colombo");

        doThrow(new ResourceAccessException("Account Service unavailable"))
                .when(accountServiceClient).validatePassenger(1L);

        assertThrows(ResourceAccessException.class, () -> rideService.createRide(request));
        verify(rideRepository, never()).save(any(Ride.class));
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

        request.setServiceArea("Colombo");

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(driverServiceClient.findAvailableDrivers("Colombo"))
                .thenReturn(List.of(new AvailableDriverResponse(101L)));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result =
                rideService.assignDriver(1L, request);

        assertEquals(101L, result.getDriverId());
        assertEquals(
                RideStatus.ASSIGNED,
                result.getStatus()
        );

        InOrder order = inOrder(driverServiceClient, rideRepository);
        order.verify(driverServiceClient).findAvailableDrivers("Colombo");
        order.verify(driverServiceClient).markOnRide(101L);
        order.verify(rideRepository).save(ride);
    }

    @Test
    void assignDriverShouldNotUpdateRideWhenNoDriverIsAvailable() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.REQUESTED);
        AssignDriverRequest request = new AssignDriverRequest();
        request.setServiceArea("Colombo");

        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(driverServiceClient.findAvailableDrivers("Colombo")).thenReturn(List.of());

        assertThrows(ResponseStatusException.class,
                () -> rideService.assignDriver(1L, request));

        assertNull(ride.getDriverId());
        assertEquals(RideStatus.REQUESTED, ride.getStatus());
        verify(driverServiceClient, never()).markOnRide(any());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void assignDriverShouldNotUpdateRideWhenDriverServicePatchFails() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.REQUESTED);
        AssignDriverRequest request = new AssignDriverRequest();
        request.setServiceArea("Colombo");

        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(driverServiceClient.findAvailableDrivers("Colombo"))
                .thenReturn(List.of(new AvailableDriverResponse(101L)));
        doThrow(new IllegalStateException("Driver Service unavailable"))
                .when(driverServiceClient).markOnRide(101L);

        assertThrows(IllegalStateException.class,
                () -> rideService.assignDriver(1L, request));

        assertNull(ride.getDriverId());
        assertEquals(RideStatus.REQUESTED, ride.getStatus());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    // Test invalid driver assignment
    @Test
    void assignDriverShouldFailWhenRideIsNotRequested() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);

        AssignDriverRequest request =
                new AssignDriverRequest();

        request.setServiceArea("Colombo");

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        assertThrows(
                InvalidRideStatusException.class,
                () -> rideService.assignDriver(1L, request)
        );

        verify(rideRepository, never())
                .save(any(Ride.class));
        verifyNoInteractions(driverServiceClient);
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
    void completeRideShouldCreateFinalFareReleaseDriverAndChangeStatus() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setDriverId(101L);
        ride.setPickupLocation("Colombo Fort");
        ride.setDestination("Bambalapitiya");
        CompleteRideRequest request = completionRequest(5.0);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result =
                rideService.completeRide(1L, request);

        assertEquals(
                RideStatus.COMPLETED,
                result.getStatus()
        );

        FinalFareRequest expectedFare = new FinalFareRequest(
                1L, 5.0, "Colombo Fort", "Bambalapitiya");
        InOrder order = inOrder(farePaymentServiceClient, driverServiceClient, rideRepository);
        order.verify(farePaymentServiceClient).createFinalFare(expectedFare);
        order.verify(driverServiceClient).markAvailable(101L);
        order.verify(rideRepository).save(ride);
    }

    @Test
    void completeRideShouldNotUpdateRideWhenFareServiceFails() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setDriverId(101L);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        doThrow(new IllegalStateException("Fare Service unavailable"))
                .when(farePaymentServiceClient).createFinalFare(any(FinalFareRequest.class));

        assertThrows(IllegalStateException.class,
                () -> rideService.completeRide(1L, completionRequest(5.0)));

        assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());
        verifyNoInteractions(driverServiceClient);
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void completeRideShouldNotUpdateRideWhenDriverReleaseFails() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setDriverId(101L);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        doThrow(new IllegalStateException("Driver Service unavailable"))
                .when(driverServiceClient).markAvailable(101L);

        assertThrows(IllegalStateException.class,
                () -> rideService.completeRide(1L, completionRequest(5.0)));

        assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());
        verify(farePaymentServiceClient).createFinalFare(any(FinalFareRequest.class));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void completeRideShouldRejectRideThatIsNotInProgress() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.ACCEPTED);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStatusException.class,
                () -> rideService.completeRide(1L, completionRequest(5.0)));

        verifyNoInteractions(farePaymentServiceClient, driverServiceClient);
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void completeRideRequestRequiresPositiveDistance() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            assertFalse(validator.validate(completionRequest(null)).isEmpty());
            assertFalse(validator.validate(completionRequest(0.0)).isEmpty());
            assertFalse(validator.validate(completionRequest(-1.0)).isEmpty());
            assertTrue(validator.validate(completionRequest(5.0)).isEmpty());
        }
    }

    @Test
    void createPaymentForCompletedRideUsesFareAndPassenger() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setPassengerId(42L);
        PaymentRideRequest request = new PaymentRideRequest();
        PaymentCreationRequest expectedRequest = new PaymentCreationRequest(
                1L, 42L, 550.0, "SIMULATED_CARD", false);
        PaymentResponse expectedResponse = paymentResponse("SUCCESS");

        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(farePaymentServiceClient.getFinalFare(1L)).thenReturn(new FareLookupResponse(550.0));
        when(farePaymentServiceClient.getPaymentsByRideId(1L)).thenReturn(List.of());
        when(farePaymentServiceClient.createPayment(expectedRequest)).thenReturn(expectedResponse);

        PaymentResponse result = rideService.createPayment(1L, request);

        assertSame(expectedResponse, result);
        InOrder order = inOrder(farePaymentServiceClient);
        order.verify(farePaymentServiceClient).getFinalFare(1L);
        order.verify(farePaymentServiceClient).getPaymentsByRideId(1L);
        order.verify(farePaymentServiceClient).createPayment(expectedRequest);
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void createPaymentRejectsRideThatIsNotCompleted() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStatusException.class,
                () -> rideService.createPayment(1L, new PaymentRideRequest()));

        verifyNoInteractions(farePaymentServiceClient);
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void createPaymentFailsWhenFinalFareIsMissing() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(farePaymentServiceClient.getFinalFare(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Final fare not found"));

        assertThrows(ResponseStatusException.class,
                () -> rideService.createPayment(1L, new PaymentRideRequest()));

        verify(farePaymentServiceClient, never()).createPayment(any(PaymentCreationRequest.class));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void createPaymentFailsWhenFarePaymentServiceIsUnavailable() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(farePaymentServiceClient.getFinalFare(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Fare & Payment Service is unavailable"));

        assertThrows(ResponseStatusException.class,
                () -> rideService.createPayment(1L, new PaymentRideRequest()));

        verify(farePaymentServiceClient, never()).createPayment(any(PaymentCreationRequest.class));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void createPaymentRejectsDuplicateSuccessfulPayment() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(farePaymentServiceClient.getFinalFare(1L)).thenReturn(new FareLookupResponse(550.0));
        when(farePaymentServiceClient.getPaymentsByRideId(1L))
                .thenReturn(List.of(paymentResponse("SUCCESS")));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> rideService.createPayment(1L, new PaymentRideRequest()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(farePaymentServiceClient, never()).createPayment(any(PaymentCreationRequest.class));
        verify(rideRepository, never()).save(any(Ride.class));
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
        verifyNoInteractions(driverServiceClient);
    }

    @Test
    void cancelRideShouldReleaseAssignedDriver() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setDriverId(101L);

        when(rideRepository.findById(2L)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ride result = rideService.cancelRide(2L);

        assertEquals(RideStatus.CANCELLED, result.getStatus());
        InOrder order = inOrder(driverServiceClient, rideRepository);
        order.verify(driverServiceClient).markAvailable(101L);
        order.verify(rideRepository).save(ride);
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

    private static CompleteRideRequest completionRequest(Double distanceKm) {
        CompleteRideRequest request = new CompleteRideRequest();
        request.setDistanceKm(distanceKm);
        return request;
    }

    private static PaymentResponse paymentResponse(String status) {
        return new PaymentResponse(7L, 1L, 42L, 550.0,
                "SIMULATED_CARD", status, "TXN-7", "2026-09-17T12:00:00");
    }
}
