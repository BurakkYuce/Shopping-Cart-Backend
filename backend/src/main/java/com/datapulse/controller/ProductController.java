package com.datapulse.controller;

import com.datapulse.dto.request.CreateProductRequest;
import com.datapulse.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> getProducts(
            @Nullable Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false, defaultValue = "name,asc") String sort) {
        boolean hasFilters = q != null || categoryId != null || brand != null || minPrice != null || maxPrice != null || storeId != null;
        Sort sortObj = parseSort(sort, hasFilters);
        PageRequest pageable = PageRequest.of(page, size, sortObj);
        if (hasFilters) {
            return ResponseEntity.ok(productService.searchProducts(q, categoryId, brand, minPrice, maxPrice, storeId, pageable));
        }
        return ResponseEntity.ok(productService.getProducts(auth, pageable));
    }

    private Sort parseSort(String sort, boolean nativeQuery) {
        try {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (nativeQuery) {
                field = field.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            }
            Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(direction, field);
        } catch (Exception e) {
            return Sort.by("name").ascending();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<?> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request, auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<?> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody CreateProductRequest request,
            Authentication auth) {
        return ResponseEntity.ok(productService.updateProduct(id, request, auth));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id, Authentication auth) {
        productService.deleteProduct(id, auth);
        return ResponseEntity.noContent().build();
    }
}
