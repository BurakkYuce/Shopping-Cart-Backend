package com.datapulse.controller;

import com.datapulse.dto.request.AddToCartRequest;
import com.datapulse.dto.request.CheckoutRequest;
import com.datapulse.dto.request.CreateOrderRequest;
import com.datapulse.dto.response.CartResponse;
import com.datapulse.dto.response.OrderResponse;
import com.datapulse.service.CartService;
import com.datapulse.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INDIVIDUAL')")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCart(auth));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication auth) {
        return ResponseEntity.ok(cartService.addToCart(request, auth));
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable String productId,
            @RequestBody Map<String, Integer> body,
            Authentication auth) {
        int quantity = body.getOrDefault("quantity", 1);
        return ResponseEntity.ok(cartService.updateQuantity(productId, quantity, auth));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable String productId,
            Authentication auth) {
        cartService.removeFromCart(productId, auth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication auth) {
        cartService.clearCart(auth);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication auth) {
        CreateOrderRequest orderRequest = cartService.toOrderRequest(body.getStoreId(), body.getPaymentMethod(), auth);
        orderRequest.setKvkkConsent(body.getKvkkConsent());
        orderRequest.setDistanceSaleConsent(body.getDistanceSaleConsent());
        orderRequest.setPreInformationConsent(body.getPreInformationConsent());
        OrderResponse order = orderService.createOrder(orderRequest, idempotencyKey, auth);

        cartService.clearCart(auth);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
