package com.datapulse.controller;

import com.datapulse.model.OrderItem;
import com.datapulse.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-items")
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderItemRepository orderItemRepository;

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<List<OrderItem>> getByOrderId(
            @PathVariable String orderId,
            Authentication auth) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return ResponseEntity.ok(items);
    }
}
