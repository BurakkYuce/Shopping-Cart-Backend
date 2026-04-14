package com.datapulse.controller;

import com.datapulse.dto.request.CreateOrderRequest;
import com.datapulse.service.OrderExportService;
import com.datapulse.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderExportService orderExportService;

    @GetMapping
    public ResponseEntity<?> getOrders(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getOrders(auth, pageable, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(orderService.getOrderById(id, auth));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INDIVIDUAL','ADMIN')")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, idempotencyKey, auth));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, body.get("status"), auth));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(orderService.cancelOrder(id, reason, auth));
    }

    @PostMapping("/{id}/return-requests")
    public ResponseEntity<?> createReturnRequest(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.get("reason") : "No reason provided";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createReturnRequest(id, reason, auth));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<?> getTracking(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(orderService.getTracking(id, auth));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(defaultValue = "csv") String format,
            Authentication auth) {
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = orderExportService.exportPdf(auth);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        }
        byte[] csv = orderExportService.exportCsv(auth);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
