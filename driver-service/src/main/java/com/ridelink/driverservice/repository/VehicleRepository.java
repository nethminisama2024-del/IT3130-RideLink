package com.ridelink.driverservice.repository;

import com.ridelink.driverservice.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByDriverId(Long driverId);

    boolean existsByDriverId(Long driverId);

    boolean existsByRegistrationNumber(String registrationNumber);
}