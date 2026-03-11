package com.datapulse.controller;

import com.datapulse.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sales")
    public ResponseEntity<?> getSalesAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Authentication auth) {
        return ResponseEntity.ok(analyticsService.getSalesAnalytics(from, to, auth));
    }

    @GetMapping("/customers")
    public ResponseEntity<?> getCustomerAnalytics(Authentication auth) {
        return ResponseEntity.ok(analyticsService.getCustomerAnalytics(auth));
    }

    @GetMapping("/products")
    public ResponseEntity<?> getProductAnalytics(
            @RequestParam(required = false) String storeId,
            Authentication auth) {
        return ResponseEntity.ok(analyticsService.getProductAnalytics(storeId, auth));
    }
}
