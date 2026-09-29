package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.entity.Payment;
import com.ridelink.farepayment.entity.PaymentStatus;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setRideId(request.getRideId());
        payment.setPassengerId(request.getPassengerId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod() != null
                ? request.getPaymentMethod() : "SIMULATED_CARD");

        if (Boolean.TRUE.equals(request.getSimulateFailure())) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setTransactionReference("FAILED-" + UUID.randomUUID());
        } else {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionReference("TXN-" + UUID.randomUUID());
        }

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByRideId(Long rideId) {
        return paymentRepository.findByRideId(rideId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ReceiptResponse getReceipt(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        return new ReceiptResponse(
                payment.getId(),
                payment.getRideId(),
                payment.getPassengerId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus().name(),
                payment.getCreatedAt()
        );
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getRideId(),
                payment.getPassengerId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus().name(),
                payment.getTransactionReference(),
                payment.getCreatedAt()
        );
    }
}