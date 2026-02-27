package com.burak.dream_shops.controller;


import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Cart;
import com.burak.dream_shops.request.ApiResponse;
import com.burak.dream_shops.service.cart.ICartService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.resolve;

@RequiredArgsConstructor
@RestController
@RequestMapping ("${api.prefix}/carts")
public class CartController {
    private final ICartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse> getCart(@PathVariable Long cartId){
        try {
            Cart cart = cartService.getCart(cartId);
            return ResponseEntity.ok(new ApiResponse("Success!",cart));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));

        }
    }
    public ResponseEntity<ApiResponse>clearCart(@PathVariable Long cartId){
        cartService.clearCart(cartId);
        return  ResponseEntity.ok(new ApiResponse("Clear Cart success! ",null));
    }
    @GetMapping("/cartId}/cart/total-price")
    public ResponseEntity<ApiResponse>getTotalAmount(@PathVariable Long cartId){
        try {
            BigDecimal totalPrice=cartService.getTotalPrice(cartId);
            return ResponseEntity.ok(new ApiResponse("Total Price",totalPrice));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }
    }
}
