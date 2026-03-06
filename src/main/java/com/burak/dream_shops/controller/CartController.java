package com.burak.dream_shops.controller;


import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Cart;
import com.burak.dream_shops.request.ApiResponse;
import com.burak.dream_shops.service.cart.ICartService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.resolve;

/**
 * Sepet (Cart) işlemlerini yöneten controller.
 * Sepet görüntüleme, temizleme ve toplam fiyat sorgulama endpoint'lerini içerir.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/carts")
public class CartController {
    private final ICartService cartService;

    /** GET /api/v1/carts/{cartId}/my-cart → Sepeti ve içindeki ürünleri getirir. */
    @GetMapping("/{cartId}/my-cart")
    public ResponseEntity<ApiResponse> getCart(@PathVariable Long cartId){
        try {
            Cart cart = cartService.getCart(cartId);
            return ResponseEntity.ok(new ApiResponse("Success!",cart));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));

        }
    }
    /** DELETE /api/v1/carts/{cartId}/clear → Sepetteki tüm ürünleri siler ve sepeti kaldırır. */
    @DeleteMapping("/{cartId}/clear")
    public ResponseEntity<ApiResponse>clearCart(@PathVariable Long cartId){
        try {
            cartService.clearCart(cartId);
            return  ResponseEntity.ok(new ApiResponse("Clear Cart success! ",null));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }
    }
    /** GET /api/v1/carts/{cartId}/cart/total-price → Sepet toplam tutarını döner. */
    @GetMapping("/{cartId}/cart/total-price")
    public ResponseEntity<ApiResponse>getTotalAmount(@PathVariable Long cartId){
        try {
            BigDecimal totalPrice=cartService.getTotalPrice(cartId);
            return ResponseEntity.ok(new ApiResponse("Total Price",totalPrice));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }
    }
}
