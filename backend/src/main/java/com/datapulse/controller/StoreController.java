package com.datapulse.controller;

import com.datapulse.dto.request.CreateStoreRequest;
import com.datapulse.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<?> getStores(Authentication auth) {
        return ResponseEntity.ok(storeService.getStores(auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStoreById(@PathVariable String id) {
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @PostMapping
    public ResponseEntity<?> createStore(
            @Valid @RequestBody CreateStoreRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request, auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStore(
            @PathVariable String id,
            @Valid @RequestBody CreateStoreRequest request,
            Authentication auth) {
        return ResponseEntity.ok(storeService.updateStore(id, request, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable String id, Authentication auth) {
        storeService.deleteStore(id, auth);
        return ResponseEntity.noContent().build();
    }
}
