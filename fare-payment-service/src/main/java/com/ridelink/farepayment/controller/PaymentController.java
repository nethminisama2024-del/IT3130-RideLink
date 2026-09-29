package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Simulated payment and receipt APIs")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Process a simulated payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get all payments for a ride")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(paymentService.getPaymentsByRideId(rideId));
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Get receipt for a payment")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getReceipt(id));
    }
}