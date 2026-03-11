package com.datapulse.controller;

import com.datapulse.dto.request.CreateReviewRequest;
import com.datapulse.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
