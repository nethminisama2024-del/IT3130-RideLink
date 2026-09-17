package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.VehicleRequest;
import com.ridelink.driverservice.entity.Vehicle;
import com.ridelink.driverservice.enums.VehicleType;
import com.ridelink.driverservice.exception.BusinessRuleException;
import com.ridelink.driverservice.exception.DriverNotFoundException;
import com.ridelink.driverservice.exception.DuplicateVehicleRegistrationException;
import com.ridelink.driverservice.exception.VehicleNotFoundException;
import com.ridelink.driverservice.repository.DriverRepository;
import com.ridelink.driverservice.repository.VehicleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle vehicle;
    private VehicleRequest request;

    @BeforeEach
    void setUp() {

        vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setDriverId(1L);
        vehicle.setRegistrationNumber("CAB-1234");
        vehicle.setMake("Toyota");
        vehicle.setModel("Aqua");
        vehicle.setColour("White");
        vehicle.setVehicleType(VehicleType.CAR);

        request = new VehicleRequest();
        request.setRegistrationNumber("CAB-1234");
        request.setMake("Toyota");
        request.setModel("Aqua");
        request.setColour("White");
        request.setVehicleType(VehicleType.CAR);
    }

    @Test
    void addVehicle_success() {

        when(driverRepository.existsById(1L))
                .thenReturn(true);

        when(vehicleRepository.existsByDriverId(1L))
                .thenReturn(false);

        when(vehicleRepository.existsByRegistrationNumber("CAB-1234"))
                .thenReturn(false);

        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle result =
                vehicleService.addVehicle(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getDriverId());
        assertEquals("CAB-1234", result.getRegistrationNumber());
        assertEquals("Toyota", result.getMake());
        assertEquals(VehicleType.CAR, result.getVehicleType());

        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void addVehicle_invalidDriver_shouldThrowException() {

        when(driverRepository.existsById(999L))
                .thenReturn(false);

        assertThrows(
                DriverNotFoundException.class,
                () -> vehicleService.addVehicle(999L, request)
        );

        verify(vehicleRepository, never())
                .save(any(Vehicle.class));
    }

    @Test
    void addSecondVehicleForSameDriver_shouldFail() {

        when(driverRepository.existsById(1L))
                .thenReturn(true);

        when(vehicleRepository.existsByDriverId(1L))
                .thenReturn(true);

        assertThrows(
                BusinessRuleException.class,
                () -> vehicleService.addVehicle(1L, request)
        );

        verify(vehicleRepository, never())
                .save(any(Vehicle.class));
    }

    @Test
    void duplicateRegistration_shouldFail() {

        when(driverRepository.existsById(1L))
                .thenReturn(true);

        when(vehicleRepository.existsByDriverId(1L))
                .thenReturn(false);

        when(vehicleRepository.existsByRegistrationNumber("CAB-1234"))
                .thenReturn(true);

        assertThrows(
                DuplicateVehicleRegistrationException.class,
                () -> vehicleService.addVehicle(1L, request)
        );

        verify(vehicleRepository, never())
                .save(any(Vehicle.class));
    }

    @Test
    void getVehicle_success() {

        when(driverRepository.existsById(1L))
                .thenReturn(true);

        when(vehicleRepository.findByDriverId(1L))
                .thenReturn(Optional.of(vehicle));

        Vehicle result =
                vehicleService.getVehicleByDriver(1L);

        assertEquals("CAB-1234", result.getRegistrationNumber());
        assertEquals("Toyota", result.getMake());
    }

    @Test
    void getVehicle_notFound_shouldThrowException() {

        when(driverRepository.existsById(1L))
                .thenReturn(true);

        when(vehicleRepository.findByDriverId(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                VehicleNotFoundException.class,
                () -> vehicleService.getVehicleByDriver(1L)
        );
    }

    @Test
    void updateVehicle_success() {

        VehicleRequest updateRequest = new VehicleRequest();
        updateRequest.setRegistrationNumber("CAB-1234");
        updateRequest.setMake("Toyota");
        updateRequest.setModel("Prius");
        updateRequest.setColour("Silver");
        updateRequest.setVehicleType(VehicleType.CAR);

        when(driverRepository.existsById(1L))
                .thenReturn(true);

        when(vehicleRepository.findByDriverId(1L))
                .thenReturn(Optional.of(vehicle));

        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle result =
                vehicleService.updateVehicle(1L, updateRequest);

        assertEquals("Prius", result.getModel());
        assertEquals("Silver", result.getColour());

        verify(vehicleRepository).save(vehicle);
    }
}