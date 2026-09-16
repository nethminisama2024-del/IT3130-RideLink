package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.entity.Payment;
import com.ridelink.farepayment.entity.PaymentStatus;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        // Payment request
        paymentRequest = new PaymentRequest();
        paymentRequest.setRideId(1001L);
        paymentRequest.setPassengerId(1L);
        paymentRequest.setAmount(950.0);
        paymentRequest.setPaymentMethod("SIMULATED_CARD");
        paymentRequest.setSimulateFailure(false);

        // Saved payment (mock return value)
        savedPayment = new Payment();
        savedPayment.setId(501L);
        savedPayment.setRideId(1001L);
        savedPayment.setPassengerId(1L);
        savedPayment.setAmount(950.0);
        savedPayment.setPaymentMethod("SIMULATED_CARD");
        savedPayment.setPaymentStatus(PaymentStatus.SUCCESS);
        savedPayment.setTransactionReference("TXN-test-123");
        savedPayment.setCreatedAt(LocalDateTime.now());
    }

    // ============================================
    // TEST 1: Successful payment
    // ============================================
    @Test
    @DisplayName("Should process payment successfully")
    void testProcessPayment_Success() {
        // Mock repository save
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // When
        PaymentResponse response = paymentService.processPayment(paymentRequest);

        // Then
        assertNotNull(response);
        assertEquals(501L, response.getPaymentId());
        assertEquals(1001L, response.getRideId());
        assertEquals(950.0, response.getAmount());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("SIMULATED_CARD", response.getPaymentMethod());

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    // ============================================
    // TEST 2: simulateFailure
    // ============================================
    @Test
    @DisplayName("Should process failed payment when simulateFailure is true")
    void testProcessPayment_Failure() {
        paymentRequest.setSimulateFailure(true);

        Payment failedPayment = new Payment();
        failedPayment.setId(502L);
        failedPayment.setRideId(1001L);
        failedPayment.setPassengerId(1L);
        failedPayment.setAmount(950.0);
        failedPayment.setPaymentMethod("SIMULATED_CARD");
        failedPayment.setPaymentStatus(PaymentStatus.FAILED);
        failedPayment.setTransactionReference("FAILED-test-456");
        failedPayment.setCreatedAt(LocalDateTime.now());

        when(paymentRepository.save(any(Payment.class))).thenReturn(failedPayment);

        PaymentResponse response = paymentService.processPayment(paymentRequest);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getTransactionReference().startsWith("FAILED-"));
    }

    // ============================================
    // TEST 3:
    // ============================================
    @Test
    @DisplayName("Should retrieve payment by ID")
    void testGetPaymentById_Found() {
        when(paymentRepository.findById(501L)).thenReturn(Optional.of(savedPayment));

        PaymentResponse response = paymentService.getPaymentById(501L);

        assertNotNull(response);
        assertEquals(501L, response.getPaymentId());
        assertEquals("SUCCESS", response.getStatus());

        verify(paymentRepository, times(1)).findById(501L);
    }

    // ============================================
    // TEST 4:
    // ============================================
    @Test
    @DisplayName("Should throw exception when payment not found")
    void testGetPaymentById_NotFound_ThrowsException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.getPaymentById(999L)
        );

        assertEquals("Payment not found with id: 999", exception.getMessage());
    }

    // ============================================
    // TEST 5:
    // ============================================
    @Test
    @DisplayName("Should retrieve all payments for a ride")
    void testGetPaymentsByRideId_ReturnsList() {
        Payment payment2 = new Payment();
        payment2.setId(502L);
        payment2.setRideId(1001L);
        payment2.setPassengerId(1L);
        payment2.setAmount(950.0);
        payment2.setPaymentStatus(PaymentStatus.SUCCESS);
        payment2.setPaymentMethod("SIMULATED_CARD");
        payment2.setCreatedAt(LocalDateTime.now());

        when(paymentRepository.findByRideId(1001L))
                .thenReturn(Arrays.asList(savedPayment, payment2));

        List<PaymentResponse> payments = paymentService.getPaymentsByRideId(1001L);

        assertNotNull(payments);
        assertEquals(2, payments.size());
        assertEquals(1001L, payments.get(0).getRideId());
    }

    // ============================================
    // TEST 6:
    // ============================================
    @Test
    @DisplayName("Should return empty list when no payments found for ride")
    void testGetPaymentsByRideId_EmptyList() {
        when(paymentRepository.findByRideId(9999L)).thenReturn(Arrays.asList());

        List<PaymentResponse> payments = paymentService.getPaymentsByRideId(9999L);

        assertNotNull(payments);
        assertTrue(payments.isEmpty());
    }

    // ============================================
    // TEST 7:
    // ============================================
    @Test
    @DisplayName("Should generate receipt for a payment")
    void testGetReceipt_ReturnsReceipt() {
        when(paymentRepository.findById(501L)).thenReturn(Optional.of(savedPayment));

        ReceiptResponse receipt = paymentService.getReceipt(501L);

        assertNotNull(receipt);
        assertEquals(501L, receipt.getReceiptId());
        assertEquals(1001L, receipt.getRideId());
        assertEquals(950.0, receipt.getAmount());
        assertEquals("SUCCESS", receipt.getPaymentStatus());
        assertEquals("SIMULATED_CARD", receipt.getPaymentMethod());
        assertNotNull(receipt.getMessage());
    }

    // ============================================
    // TEST 8:
    // ============================================
    @Test
    @DisplayName("Should throw exception when generating receipt for non-existent payment")
    void testGetReceipt_PaymentNotFound_ThrowsException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.getReceipt(999L)
        );
    }
}