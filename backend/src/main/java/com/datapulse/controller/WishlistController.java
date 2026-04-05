package com.datapulse.controller;

import com.datapulse.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<?> getWishlist(Authentication auth) {
        return ResponseEntity.ok(wishlistService.getWishlist(auth));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<?> addToWishlist(@PathVariable String productId, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wishlistService.addToWishlist(productId, auth));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable String productId, Authentication auth) {
        wishlistService.removeFromWishlist(productId, auth);
        return ResponseEntity.noContent().build();
    }
}
