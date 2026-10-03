package com.ridelink.farepayment.dto;

import java.time.LocalDateTime;

public class PaymentResponse {
    private Long paymentId;
    private Long rideId;
    private Long passengerId;
    private Double amount;
    private String paymentMethod;
    private String status;
    private String transactionReference;
    private LocalDateTime createdAt;

    public PaymentResponse(Long paymentId, Long rideId, Long passengerId, Double amount,
                           String paymentMethod, String status, String transactionReference,
                           LocalDateTime createdAt) {
        this.paymentId = paymentId;
        this.rideId = rideId;
        this.passengerId = passengerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionReference = transactionReference;
        this.createdAt = createdAt;
    }

    public Long getPaymentId() { return paymentId; }
    public Long getRideId() { return rideId; }
    public Long getPassengerId() { return passengerId; }
    public Double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public String getTransactionReference() { return transactionReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}