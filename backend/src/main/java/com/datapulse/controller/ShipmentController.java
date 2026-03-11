package com.datapulse.controller;

import com.datapulse.model.Shipment;
import com.datapulse.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok(shipmentService.getById(id));
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<?> getByOrderId(@PathVariable String orderId, Authentication auth) {
        return ResponseEntity.ok(shipmentService.getByOrderId(orderId, auth));
    }

    @PostMapping
    public ResponseEntity<?> createShipment(
            @RequestBody Shipment shipment,
            Authentication auth) {
        String orderId = shipment.getOrderId();
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.createShipment(orderId, shipment, auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateShipment(
            @PathVariable String id,
            @RequestBody Shipment body,
            Authentication auth) {
        return ResponseEntity.ok(shipmentService.updateShipment(id, body, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(@PathVariable String id, Authentication auth) {
        shipmentService.deleteShipment(id, auth);
        return ResponseEntity.noContent().build();
    }
}
