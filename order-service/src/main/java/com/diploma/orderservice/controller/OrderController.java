package com.diploma.orderservice.controller;

import com.diploma.orderservice.dto.CreateOrderRequest;
import com.diploma.orderservice.entity.Order;
import com.diploma.orderservice.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public List<Order> getOrders(@AuthenticationPrincipal Jwt jwt) {
        if (hasRole(jwt, "admin")) {
            return orderRepository.findAll();
        }
        return orderRepository.findByUserId(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return orderRepository.findById(id)
                .map(order -> {
                    if (order.getUserId().equals(jwt.getSubject()) || hasRole(jwt, "admin")) {
                        return ResponseEntity.ok(order);
                    }
                    return ResponseEntity.status(403).<Order>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Order order = new Order(
                jwt.getSubject(),
                request.getDescription(),
                request.getAmount()
        );
        return ResponseEntity.status(201).body(orderRepository.save(order));
    }

    private boolean hasRole(Jwt jwt, String role) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        var roles = (List<String>) realmAccess.get("roles");
        return roles != null && roles.contains(role);
    }
}
