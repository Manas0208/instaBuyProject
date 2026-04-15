package com.mphasis.instabuy.paymentservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mphasis.instabuy.paymentservice.dto.PaymentRequest;
import com.mphasis.instabuy.paymentservice.dto.PaymentResponse;
import com.mphasis.instabuy.paymentservice.service.PaymentService;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/pay")
    public PaymentResponse pay(@RequestBody PaymentRequest request) {
        return paymentService.processPayment(
                request.getUserId(),
                request.getOrderId(),
                request.getAmount()
        );
    }

    @PostMapping("/refund")
    public PaymentResponse refund(@RequestBody PaymentRequest request) {
        return paymentService.refund(
                request.getUserId(),
                request.getOrderId(),
                request.getAmount()
        );
    }
}
