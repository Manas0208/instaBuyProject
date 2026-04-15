package com.mphasis.instabuy.orderservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.mphasis.instabuy.orderservice.cart.CartManager;
import com.mphasis.instabuy.orderservice.client.InventoryClient;
import com.mphasis.instabuy.orderservice.client.PaymentClient;
import com.mphasis.instabuy.orderservice.client.UserClient;
import com.mphasis.instabuy.orderservice.dto.CartItemDTO;
import com.mphasis.instabuy.orderservice.dto.OrderRequest;
import com.mphasis.instabuy.orderservice.dto.OrderResponse;
import com.mphasis.instabuy.orderservice.dto.PaymentResponse;
import com.mphasis.instabuy.orderservice.entity.Order;
import com.mphasis.instabuy.orderservice.entity.OrderItem;
import com.mphasis.instabuy.orderservice.exception.CustomException;
import com.mphasis.instabuy.orderservice.repository.OrderItemRepository;
import com.mphasis.instabuy.orderservice.repository.OrderRepository;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartManager cartManager;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private UserClient userClient;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Override
    public OrderResponse placeOrder(OrderRequest request, Long userId) {

        List<CartItemDTO> cartItems = cartManager.getCart(userId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus("CREATED");
        order.setShippingAddress(request.getShippingAddress());
        order.setPhone(request.getPhone());

        double total = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemDTO c : cartItems) {
            boolean stock = inventoryClient.checkStock(c.getProductId(), c.getQuantity());

            if (!stock) {
                throw new RuntimeException("Out of stock for product: " + c.getProductId());
            }
        }

        for (CartItemDTO c : cartItems) {
            OrderItem item = new OrderItem();
            item.setProductId(c.getProductId());
            item.setQuantity(c.getQuantity());
            item.setPrice(c.getPrice());
            item.setTotalPrice(c.getPrice() * c.getQuantity());
            item.setOrder(order);

            total += item.getTotalPrice();
            orderItems.add(item);
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);

        // SAVE ORDER FIRST
        Order saved = orderRepository.save(order);

        // PAYMENT CALL
        PaymentResponse payment = paymentClient.processPayment(
                userId,
                saved.getOrderId(),
                total
        );

        if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            OrderResponse res = new OrderResponse();
            res.setStatus("FAILED");
            return res;
        }

        // REDUCE STOCK AFTER PAYMENT
        for (CartItemDTO c : cartItems) {
            inventoryClient.reduceStock(c.getProductId(), c.getQuantity());
        }

        saved.setPaymentStatus("SUCCESS");
        saved.setOrderStatus("CONFIRMED");

        orderRepository.save(saved);

        OrderResponse res = new OrderResponse();
        res.setAmount(total);
        res.setCartList(cartItems);
        res.setStatus(saved.getOrderStatus());
        res.setOrderId(saved.getOrderId());

        cartManager.clearCart(userId);

        return res;
    }

    @Override
    public Order updateDetails(Long orderId, String address, long phone) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found"));

        order.setShippingAddress(address);
        order.setPhone(phone);

        return orderRepository.save(order);
    }

    @Override
    public String cancelOrder(Long orderId) {
    		
    	System.out.println("Cancel called for orderId: " + orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found"));

        // already cancelled
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return "Already cancelled";
        }

        // 🔥 only confirmed orders can cancel
        if (!"CONFIRMED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Only confirmed orders can be cancelled");
        }

        // 🔥 restore stock
        for (OrderItem item : order.getItems()) {
            inventoryClient.increaseStock(
                    item.getProductId(),
                    item.getQuantity()
            );
        }

        // 🔥 ONLY CANCEL (NO REFUND HERE)
        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus("PENDING");;

        orderRepository.save(order);

        return "Order cancelled successfully. Waiting for admin refund.";
    }
    @Override
    public Order updateStatus(Long orderId, String status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found"));

        order.setOrderStatus(status);

        return orderRepository.save(order);
    }
    
    @Override
    public String refundOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found"));

        if (!"CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Refund allowed only for cancelled orders");
        }

        if ("REFUND_DONE".equalsIgnoreCase(order.getPaymentStatus())) {
            return "Already refunded";
        }

        // 🔥 CALL PAYMENT SERVICE
        paymentClient.refund(
                order.getUserId(),
                order.getOrderId(),
                order.getTotalAmount()
        );

        // UPDATE STATUS
        order.setPaymentStatus("REFUNDED");

        orderRepository.save(order);

        return "Refund successful";
    }
    @Override
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    @Override
    public List<OrderItem> getItemByOrderId(Long orderId) {
        return orderItemRepository.findByOrderOrderId(orderId);
    }
}