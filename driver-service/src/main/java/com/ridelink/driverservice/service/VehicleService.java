package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.VehicleRequest;
import com.ridelink.driverservice.entity.Vehicle;
import com.ridelink.driverservice.exception.BusinessRuleException;
import com.ridelink.driverservice.exception.DriverNotFoundException;
import com.ridelink.driverservice.exception.DuplicateVehicleRegistrationException;
import com.ridelink.driverservice.exception.VehicleNotFoundException;
import com.ridelink.driverservice.repository.DriverRepository;
import com.ridelink.driverservice.repository.VehicleRepository;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository) {

        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public Vehicle addVehicle(
            Long driverId,
            VehicleRequest request) {

        if (!driverRepository.existsById(driverId)) {
            throw new DriverNotFoundException(driverId);
        }

        if (vehicleRepository.existsByDriverId(driverId)) {
            throw new BusinessRuleException(
                    "A vehicle is already registered for driver id: "
                            + driverId
            );
        }

        if (vehicleRepository.existsByRegistrationNumber(
                request.getRegistrationNumber())) {

            throw new DuplicateVehicleRegistrationException(
                    request.getRegistrationNumber()
            );
        }

        Vehicle vehicle = new Vehicle();

        vehicle.setDriverId(driverId);
        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setColour(request.getColour());
        vehicle.setVehicleType(request.getVehicleType());

        return vehicleRepository.save(vehicle);
    }

    public Vehicle getVehicleByDriver(Long driverId) {

        if (!driverRepository.existsById(driverId)) {
            throw new DriverNotFoundException(driverId);
        }

        return vehicleRepository.findByDriverId(driverId)
                .orElseThrow(() ->
                        new VehicleNotFoundException(driverId)
                );
    }

    public Vehicle updateVehicle(
            Long driverId,
            VehicleRequest request) {

        Vehicle vehicle = getVehicleByDriver(driverId);

        if (!vehicle.getRegistrationNumber()
                .equalsIgnoreCase(request.getRegistrationNumber())
                && vehicleRepository.existsByRegistrationNumber(
                        request.getRegistrationNumber())) {

            throw new DuplicateVehicleRegistrationException(
                    request.getRegistrationNumber()
            );
        }

        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setColour(request.getColour());
        vehicle.setVehicleType(request.getVehicleType());

        return vehicleRepository.save(vehicle);
    }
}