package com.datapulse.controller;

import com.datapulse.dto.request.CouponValidateRequest;
import com.datapulse.dto.request.CreateCouponRequest;
import com.datapulse.dto.response.CouponResponse;
import com.datapulse.dto.response.CouponValidationResponse;
import com.datapulse.security.UserDetailsImpl;
import com.datapulse.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // --- Public (authenticated) ---

    @PostMapping("/api/coupons/validate")
    public ResponseEntity<CouponValidationResponse> validate(
            @Valid @RequestBody CouponValidateRequest req,
            Authentication auth) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        BigDecimal subtotal = BigDecimal.valueOf(req.getSubtotal());
        CouponValidationResponse resp = couponService.validateCoupon(req.getCode(), user.getId(), subtotal);
        return ResponseEntity.ok(resp);
    }

    // --- Admin CRUD ---

    @GetMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponse>> listCoupons() {
        return ResponseEntity.ok(couponService.listCoupons());
    }

    @PostMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> createCoupon(@RequestBody CreateCouponRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(req));
    }

    @PatchMapping("/api/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable String id,
            @RequestBody CreateCouponRequest req) {
        return ResponseEntity.ok(couponService.updateCoupon(id, req));
    }

    @DeleteMapping("/api/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateCoupon(@PathVariable String id) {
        couponService.deactivateCoupon(id);
        return ResponseEntity.noContent().build();
    }
}
