package com.datapulse.controller;

import com.datapulse.model.Brand;
import com.datapulse.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<List<Brand>> getAllBrands() {
        return ResponseEntity.ok(brandRepository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Brand>> searchBrands(@RequestParam String q) {
        return ResponseEntity.ok(brandRepository.findByDisplayNameContainingIgnoreCase(q));
    }
}
