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
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        if (q != null || categoryId != null || minPrice != null || maxPrice != null) {
            return ResponseEntity.ok(productService.searchProducts(q, categoryId, minPrice, maxPrice, pageable));
        }
        return ResponseEntity.ok(productService.getProducts(auth, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<?> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request, auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody CreateProductRequest request,
            Authentication auth) {
        return ResponseEntity.ok(productService.updateProduct(id, request, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id, Authentication auth) {
        productService.deleteProduct(id, auth);
        return ResponseEntity.noContent().build();
    }
}
