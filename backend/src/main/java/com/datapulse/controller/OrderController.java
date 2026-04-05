package com.datapulse.controller;

import com.datapulse.dto.request.CreateOrderRequest;
import com.datapulse.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<?> getOrders(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getOrders(auth, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(orderService.getOrderById(id, auth));
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, auth));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, body.get("status"), auth));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(orderService.cancelOrder(id, auth));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<?> returnOrder(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(orderService.returnOrder(id, auth));
    }
}
