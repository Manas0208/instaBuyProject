package com.mphasis.instabuy.paymentservice.dto;

public class PaymentRequest {

    private Long userId;
    private Long orderId;
    private Double amount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}