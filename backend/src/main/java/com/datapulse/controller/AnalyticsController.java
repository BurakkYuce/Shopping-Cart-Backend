package com.datapulse.controller;

import com.datapulse.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sales")
    public ResponseEntity<?> getSalesAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String storeId,
            Authentication auth) {
        if (groupBy != null) {
            return ResponseEntity.ok(analyticsService.getSalesWithDrillDown(groupBy, storeId, from, to, auth));
        }
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

    @GetMapping("/store-kpis/{storeId}")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN') and @storeSecurity.isOwner(authentication, #storeId)")
    public ResponseEntity<?> getStoreKpis(@PathVariable String storeId) {
        return ResponseEntity.ok(analyticsService.getStoreKpis(storeId));
    }

    @GetMapping("/customer-segments")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<?> getCustomerSegments(
            @RequestParam(required = false) String storeId,
            Authentication auth) {
        return ResponseEntity.ok(analyticsService.getCustomerSegments(storeId, auth));
    }

    @GetMapping("/store-comparison")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStoreComparison(
            @RequestParam List<String> storeIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getStoreComparison(storeIds, from, to));
    }

    @GetMapping("/platform-overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPlatformOverview() {
        return ResponseEntity.ok(analyticsService.getPlatformOverview());
    }
}
