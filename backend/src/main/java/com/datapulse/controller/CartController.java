package com.datapulse.controller;

import com.datapulse.dto.request.AddToCartRequest;
import com.datapulse.dto.request.CheckoutRequest;
import com.datapulse.dto.request.CreateOrderRequest;
import com.datapulse.dto.response.CartResponse;
import com.datapulse.dto.response.CouponValidationResponse;
import com.datapulse.dto.response.OrderResponse;
import com.datapulse.security.UserDetailsImpl;
import com.datapulse.service.CartService;
import com.datapulse.service.CouponService;
import com.datapulse.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INDIVIDUAL')")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final CouponService couponService;

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
    public ResponseEntity<List<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication auth) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        List<CreateOrderRequest> orderRequests = cartService.toOrderRequests(body.getPaymentMethod(), auth);

        // Validate and distribute coupon discount
        String couponCode = body.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            BigDecimal cartSubtotal = orderRequests.stream()
                    .flatMap(r -> r.getItems().stream())
                    .map(i -> BigDecimal.valueOf(i.getQuantity()))  // placeholder — real price calc below
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate real subtotal from products
            cartSubtotal = cartService.calculateCartSubtotal(auth);

            CouponValidationResponse validation = couponService.validateCoupon(couponCode, user.getId(), cartSubtotal);
            BigDecimal totalDiscount = BigDecimal.valueOf(validation.getDiscountAmount());

            // Distribute discount proportionally across store orders
            distributeDiscount(orderRequests, couponCode, totalDiscount, cartSubtotal, auth);
        }

        List<OrderResponse> orders = orderRequests.stream().map(req -> {
            req.setKvkkConsent(body.getKvkkConsent());
            req.setDistanceSaleConsent(body.getDistanceSaleConsent());
            req.setPreInformationConsent(body.getPreInformationConsent());
            return orderService.createOrder(req, idempotencyKey, auth);
        }).toList();

        // Record coupon usage after all orders succeed
        if (couponCode != null && !couponCode.isBlank() && !orders.isEmpty()) {
            couponService.applyCoupon(couponCode, user.getId(), orders.get(0).getId());
        }

        cartService.clearCart(auth);

        return ResponseEntity.status(HttpStatus.CREATED).body(orders);
    }

    private void distributeDiscount(List<CreateOrderRequest> orderRequests, String couponCode,
                                     BigDecimal totalDiscount, BigDecimal cartSubtotal, Authentication auth) {
        if (orderRequests.size() == 1) {
            orderRequests.get(0).setCouponCode(couponCode);
            orderRequests.get(0).setDiscountAmount(totalDiscount.doubleValue());
            return;
        }

        // Proportional distribution across stores
        BigDecimal distributed = BigDecimal.ZERO;
        for (int i = 0; i < orderRequests.size(); i++) {
            CreateOrderRequest req = orderRequests.get(i);
            req.setCouponCode(couponCode);
            if (i == orderRequests.size() - 1) {
                // Last order gets remainder to avoid rounding issues
                req.setDiscountAmount(totalDiscount.subtract(distributed).doubleValue());
            } else {
                // Calculate this order's share of the subtotal
                BigDecimal orderSubtotal = cartService.calculateOrderSubtotal(req);
                BigDecimal share = cartSubtotal.compareTo(BigDecimal.ZERO) > 0
                        ? totalDiscount.multiply(orderSubtotal).divide(cartSubtotal, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                req.setDiscountAmount(share.doubleValue());
                distributed = distributed.add(share);
            }
        }
    }
}
