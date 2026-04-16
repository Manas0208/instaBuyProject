package com.mphasis.instabuy.orderservice.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.mphasis.instabuy.orderservice.dto.PaymentResponse;

@Component
public class PaymentClient {

    @Autowired
    private RestTemplate restTemplate;

    public PaymentResponse processPayment(Long userId, Long orderId, Double amount) {

        String url = "http://localhost:8085/api/payment/pay";

        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("orderId", orderId);
        req.put("amount", amount);

        return restTemplate.postForObject(url, req, PaymentResponse.class);
    }

    public PaymentResponse refund(Long userId, Long orderId, Double amount) {

        String url = "http://localhost:8085/api/payment/refund";

        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("orderId", orderId);
        req.put("amount", amount);

        return restTemplate.postForObject(url, req, PaymentResponse.class);
    }
}