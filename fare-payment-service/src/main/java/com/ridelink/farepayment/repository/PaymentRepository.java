package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByRideId(Long rideId);
}