package com.datapulse.controller;

import com.datapulse.dto.request.CreateReviewRequest;
import com.datapulse.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/by-product/{productId}")
    public ResponseEntity<?> getByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getByProductId(productId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INDIVIDUAL','ADMIN')")
    public ResponseEntity<?> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable String id, Authentication auth) {
        reviewService.deleteReview(id, auth);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/seller-response")
    @PreAuthorize("hasAnyRole('CORPORATE','ADMIN')")
    public ResponseEntity<?> addSellerResponse(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return ResponseEntity.ok(reviewService.addSellerResponse(id, body.get("response"), auth));
    }
}
