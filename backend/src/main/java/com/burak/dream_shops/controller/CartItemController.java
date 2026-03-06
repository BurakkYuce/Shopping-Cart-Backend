package com.burak.dream_shops.controller;

import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.request.ApiResponse;
import com.burak.dream_shops.service.cart.ICartService;
import com.burak.dream_shops.service.cartItem.ICartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Sepet kalemi (CartItem) işlemlerini yöneten controller.
 * Sepete ürün ekleme, çıkarma ve güncelleme endpoint'lerini içerir.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/cartItems")
public class CartItemController {
    private final ICartItemService cartItemService;
    private final ICartService cartService;

    /**
     * POST /api/v1/cartItems/item/add?cartId=&productId=&quantity=
     * Sepete ürün ekler. cartId gönderilmezse yeni sepet oluşturulur.
     * Bu sayede misafir kullanıcılar da sepet başlatabilir.
     */
    @PostMapping("/item/add")
    public ResponseEntity<ApiResponse> addItemToCart(@RequestParam(required = false) Long cartId,
                                                     @RequestParam Long productId,
                                                     @RequestParam Integer quantity) {
        try {
            if (cartId == null) {
                // Sepet ID verilmemişse boş yeni sepet oluştur
                cartId = cartService.initializeNewCart();
            }
            cartItemService.addItemToCart(cartId, productId, quantity);
            return ResponseEntity.ok(new ApiResponse("Add Item Success", null));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }
    /**
     * DELETE /api/v1/cartItems/cart/{cartId}/item/{itemId}/remove
     * Sepetten belirli bir ürünü kaldırır. itemId burada productId'dir.
     */
    @DeleteMapping("/cart/{cartId}/item/{itemId}/remove")
    public ResponseEntity<ApiResponse>removeItemFromCart(@PathVariable Long cartId,@PathVariable Long itemId){
        try {
            cartItemService.removeItemFromCart(cartId,itemId);
            return  ResponseEntity.ok(new ApiResponse("Remove success!",null));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }
    }
    /**
     * PUT /api/v1/cartItems/cart/{cartId}/item/{itemId}/update?quantity=
     * Sepetteki ürünün miktarını günceller. itemId burada productId'dir.
     */
    @PutMapping("/cart/{cartId}/item/{itemId}/update")
    public ResponseEntity<ApiResponse>updateItemQuantity(@PathVariable Long cartId,@PathVariable Long itemId,@RequestParam Integer quantity){
        try {
            cartItemService.updateItemQuantity(cartId,itemId,quantity);
            return ResponseEntity.ok(new ApiResponse("update success",null));
        } catch (ResourcesNotFoundException e) {
        return  ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }
    }

}
