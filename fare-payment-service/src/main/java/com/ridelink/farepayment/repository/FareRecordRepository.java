package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.entity.FareRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FareRecordRepository extends JpaRepository<FareRecord, Long> {
    List<FareRecord> findByRideId(Long rideId);
}