package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.AvailabilityRequest;
import com.ridelink.driverservice.dto.DriverRequest;
import com.ridelink.driverservice.dto.DriverUpdateRequest;
import com.ridelink.driverservice.dto.LocationRequest;
import com.ridelink.driverservice.entity.Driver;
import com.ridelink.driverservice.enums.AvailabilityStatus;
import com.ridelink.driverservice.enums.OperationalStatus;
import com.ridelink.driverservice.exception.BusinessRuleException;
import com.ridelink.driverservice.exception.DriverNotFoundException;
import com.ridelink.driverservice.exception.DuplicateDriverException;
import com.ridelink.driverservice.exception.NoEligibleDriverException;
import com.ridelink.driverservice.repository.DriverRepository;
import com.ridelink.driverservice.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository) {

        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public Driver createDriver(DriverRequest request) {

        if (driverRepository.existsByAccountId(request.getAccountId())) {
            throw new DuplicateDriverException(
                    "A driver profile already exists for account ID: "
                            + request.getAccountId()
            );
        }

        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateDriverException(
                    "License number is already registered: "
                            + request.getLicenseNumber()
            );
        }

        Driver driver = new Driver();

        driver.setAccountId(request.getAccountId());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setServiceArea(request.getServiceArea());
        driver.setLatitude(request.getLatitude());
        driver.setLongitude(request.getLongitude());

        driver.setAvailability(AvailabilityStatus.UNAVAILABLE);
        driver.setOperationalStatus(OperationalStatus.ACTIVE);

        return driverRepository.save(driver);
    }

    public Driver getDriver(Long driverId) {

        return driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new DriverNotFoundException(driverId)
                );
    }

    public Driver updateDriver(
            Long driverId,
            DriverUpdateRequest request) {

        Driver driver = getDriver(driverId);

        driver.setServiceArea(request.getServiceArea());
        driver.setOperationalStatus(request.getOperationalStatus());

        if (request.getOperationalStatus() != OperationalStatus.ACTIVE) {
            driver.setAvailability(AvailabilityStatus.UNAVAILABLE);
        }

        return driverRepository.save(driver);
    }

    public Driver updateAvailability(
            Long driverId,
            AvailabilityRequest request) {

        Driver driver = getDriver(driverId);

        AvailabilityStatus requestedStatus =
                request.getAvailability();

        if (driver.getOperationalStatus() != OperationalStatus.ACTIVE
                && requestedStatus != AvailabilityStatus.UNAVAILABLE) {

            throw new BusinessRuleException(
                    "Only an active driver can become available or go on a ride."
            );
        }

        if ((requestedStatus == AvailabilityStatus.AVAILABLE
                || requestedStatus == AvailabilityStatus.ON_RIDE)
                && !vehicleRepository.existsByDriverId(driverId)) {

            throw new BusinessRuleException(
                    "Driver must have a registered vehicle before changing to this availability status."
            );
        }

        driver.setAvailability(requestedStatus);

        return driverRepository.save(driver);
    }

    public Driver updateLocation(
            Long driverId,
            LocationRequest request) {

        Driver driver = getDriver(driverId);

        driver.setLatitude(request.getLatitude());
        driver.setLongitude(request.getLongitude());

        return driverRepository.save(driver);
    }

    public List<Driver> getEligibleDrivers(String serviceArea) {

        List<Driver> drivers =
                driverRepository
                        .findByAvailabilityAndServiceAreaIgnoreCase(
                                AvailabilityStatus.AVAILABLE,
                                serviceArea
                        )
                        .stream()
                        .filter(driver ->
                                driver.getOperationalStatus()
                                        == OperationalStatus.ACTIVE)
                        .toList();

        if (drivers.isEmpty()) {
            throw new NoEligibleDriverException(serviceArea);
        }

        return drivers;
    }
}