package com.ridelink.ride.dto;

public class PaymentRideRequest {

    private String paymentMethod = "SIMULATED_CARD";
    private boolean simulateFailure;

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod == null ? "SIMULATED_CARD" : paymentMethod;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
