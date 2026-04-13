package com.datapulse.controller;

import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.ProductAttribute;
import com.datapulse.repository.ProductAttributeRepository;
import com.datapulse.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/attributes")
@RequiredArgsConstructor
public class ProductAttributeController {

    private final ProductAttributeRepository attributeRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<List<ProductAttribute>> getAttributes(@PathVariable String productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));
        return ResponseEntity.ok(attributeRepository.findByProductId(productId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<ProductAttribute> addAttribute(
            @PathVariable String productId,
            @RequestBody Map<String, String> body) {
        productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        String key = body.get("key");
        String value = body.get("value");
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var existing = attributeRepository.findByProductIdAndAttrKey(productId, key);
        if (existing.isPresent()) {
            ProductAttribute attr = existing.get();
            attr.setAttrValue(value);
            return ResponseEntity.ok(attributeRepository.save(attr));
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        ProductAttribute attr = new ProductAttribute();
        attr.setId(id);
        attr.setProductId(productId);
        attr.setAttrKey(key);
        attr.setAttrValue(value);
        return ResponseEntity.status(HttpStatus.CREATED).body(attributeRepository.save(attr));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteAttribute(
            @PathVariable String productId,
            @PathVariable String key) {
        attributeRepository.findByProductIdAndAttrKey(productId, key)
                .orElseThrow(() -> new EntityNotFoundException("ProductAttribute", key));
        attributeRepository.deleteByProductIdAndAttrKey(productId, key);
        return ResponseEntity.noContent().build();
    }
}
