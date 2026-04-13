package com.datapulse.controller;

import com.datapulse.dto.request.CreateStoreRequest;
import com.datapulse.repository.ProductRepository;
import com.datapulse.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<?> getStores(Authentication auth) {
        return ResponseEntity.ok(storeService.getStores(auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStoreById(@PathVariable String id) {
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<?> createStore(
            @Valid @RequestBody CreateStoreRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request, auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<?> updateStore(
            @PathVariable String id,
            @Valid @RequestBody CreateStoreRequest request,
            Authentication auth) {
        return ResponseEntity.ok(storeService.updateStore(id, request, auth));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<Void> deleteStore(@PathVariable String id, Authentication auth) {
        storeService.deleteStore(id, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/low-stock-products")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN') and @storeSecurity.isOwner(authentication, #id)")
    public ResponseEntity<?> getLowStockProducts(@PathVariable String id) {
        return ResponseEntity.ok(productRepository.findLowStockByStoreId(id));
    }
}
