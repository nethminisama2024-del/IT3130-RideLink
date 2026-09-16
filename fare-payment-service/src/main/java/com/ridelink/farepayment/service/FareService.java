package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.dto.FareEstimateResponse;
import com.ridelink.farepayment.dto.FinalFareRequest;
import com.ridelink.farepayment.entity.FareRecord;
import com.ridelink.farepayment.repository.FareRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class FareService {

    // Documented fare rule:
    // Fare = Base Fare + (Distance × Rate per KM)
    private static final double BASE_FARE = 150.0;
    private static final double RATE_PER_KM = 80.0;

    private final FareRecordRepository fareRecordRepository;

    public FareService(FareRecordRepository fareRecordRepository) {
        this.fareRecordRepository = fareRecordRepository;
    }

    public FareEstimateResponse estimateFare(FareEstimateRequest request) {
        double fare = calculateFare(request.getDistanceKm());
        return new FareEstimateResponse(
                request.getDistanceKm(), fare,
                request.getPickup(), request.getDestination()
        );
    }

    public FareRecord calculateFinalFare(FinalFareRequest request) {
        double fare = calculateFare(request.getDistanceKm());

        FareRecord record = new FareRecord();
        record.setRideId(request.getRideId());
        record.setDistanceKm(request.getDistanceKm());
        record.setFareAmount(fare);
        record.setPickupLocation(request.getPickup());
        record.setDestination(request.getDestination());

        return fareRecordRepository.save(record);
    }

    private double calculateFare(double distanceKm) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        return BASE_FARE + (distanceKm * RATE_PER_KM);
    }
}