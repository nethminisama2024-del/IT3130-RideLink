package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.AvailabilityRequest;
import com.ridelink.driverservice.dto.DriverRequest;
import com.ridelink.driverservice.dto.LocationRequest;
import com.ridelink.driverservice.entity.Driver;
import com.ridelink.driverservice.enums.AvailabilityStatus;
import com.ridelink.driverservice.enums.OperationalStatus;
import com.ridelink.driverservice.exception.BusinessRuleException;
import com.ridelink.driverservice.exception.DriverNotFoundException;
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
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverService driverService;

    private Driver driver;

    @BeforeEach
    void setUp() {

        driver = new Driver();
        driver.setId(1L);
        driver.setAccountId(12L);
        driver.setLicenseNumber("B1234567");
        driver.setServiceArea("Colombo");
        driver.setLatitude(6.9271);
        driver.setLongitude(79.8612);
        driver.setAvailability(AvailabilityStatus.UNAVAILABLE);
        driver.setOperationalStatus(OperationalStatus.ACTIVE);
    }

    @Test
    void createDriver_success() {

        DriverRequest request = new DriverRequest();
        request.setAccountId(12L);
        request.setLicenseNumber("B1234567");
        request.setServiceArea("Colombo");
        request.setLatitude(6.9271);
        request.setLongitude(79.8612);

        when(driverRepository.existsByAccountId(12L))
                .thenReturn(false);

        when(driverRepository.existsByLicenseNumber("B1234567"))
                .thenReturn(false);

        when(driverRepository.save(any(Driver.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Driver result = driverService.createDriver(request);

        assertNotNull(result);
        assertEquals(12L, result.getAccountId());
        assertEquals("B1234567", result.getLicenseNumber());
        assertEquals("Colombo", result.getServiceArea());
        assertEquals(AvailabilityStatus.UNAVAILABLE, result.getAvailability());
        assertEquals(OperationalStatus.ACTIVE, result.getOperationalStatus());

        verify(driverRepository).save(any(Driver.class));
    }

    @Test
    void getDriver_success() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        Driver result = driverService.getDriver(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(12L, result.getAccountId());
        assertEquals("Colombo", result.getServiceArea());
    }

    @Test
    void getDriver_invalidId_shouldThrowException() {

        when(driverRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                DriverNotFoundException.class,
                () -> driverService.getDriver(999L)
        );
    }

    @Test
    void updateAvailability_success() {

        AvailabilityRequest request = new AvailabilityRequest();
        request.setAvailability(AvailabilityStatus.AVAILABLE);

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(vehicleRepository.existsByDriverId(1L))
                .thenReturn(true);

        when(driverRepository.save(any(Driver.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Driver result =
                driverService.updateAvailability(1L, request);

        assertEquals(
                AvailabilityStatus.AVAILABLE,
                result.getAvailability()
        );

        verify(driverRepository).save(driver);
    }

    @Test
    void driverWithoutVehicle_cannotBecomeAvailable() {

        AvailabilityRequest request = new AvailabilityRequest();
        request.setAvailability(AvailabilityStatus.AVAILABLE);

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(vehicleRepository.existsByDriverId(1L))
                .thenReturn(false);

        assertThrows(
                BusinessRuleException.class,
                () -> driverService.updateAvailability(1L, request)
        );

        verify(driverRepository, never())
                .save(any(Driver.class));
    }

    @Test
    void updateLocation_success() {

        LocationRequest request = new LocationRequest();
        request.setLatitude(6.9350);
        request.setLongitude(79.8500);

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(driverRepository.save(any(Driver.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Driver result =
                driverService.updateLocation(1L, request);

        assertEquals(6.9350, result.getLatitude());
        assertEquals(79.8500, result.getLongitude());

        verify(driverRepository).save(driver);
    }

    @Test
    void inactiveDriver_cannotBecomeAvailable() {

        driver.setOperationalStatus(OperationalStatus.INACTIVE);

        AvailabilityRequest request = new AvailabilityRequest();
        request.setAvailability(AvailabilityStatus.AVAILABLE);

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        assertThrows(
                BusinessRuleException.class,
                () -> driverService.updateAvailability(1L, request)
        );

        verify(driverRepository, never())
                .save(any(Driver.class));
    }
}