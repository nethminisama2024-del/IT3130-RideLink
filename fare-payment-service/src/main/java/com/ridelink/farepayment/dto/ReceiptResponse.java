package com.ridelink.farepayment.dto;

import java.time.LocalDateTime;

public class ReceiptResponse {
    private Long receiptId;
    private Long rideId;
    private Long passengerId;
    private Double amount;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String message;

    public ReceiptResponse(Long receiptId, Long rideId, Long passengerId, Double amount,
                           String paymentMethod, String paymentStatus, LocalDateTime paymentDate) {
        this.receiptId = receiptId;
        this.rideId = rideId;
        this.passengerId = passengerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
        this.message = "Payment receipt generated successfully";
    }

    public Long getReceiptId() { return receiptId; }
    public Long getRideId() { return rideId; }
    public Long getPassengerId() { return passengerId; }
    public Double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public String getMessage() { return message; }
}