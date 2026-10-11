package com.ridelink.driverservice.repository;

import com.ridelink.driverservice.entity.Driver;
import com.ridelink.driverservice.enums.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByAccountId(Long accountId);

    boolean existsByAccountId(Long accountId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByAvailability(AvailabilityStatus availability);

    List<Driver> findByAvailabilityAndServiceAreaIgnoreCase(
            AvailabilityStatus availability,
            String serviceArea
    );
}