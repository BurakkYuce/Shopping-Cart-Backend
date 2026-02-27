package com.burak.dream_shops.service.cartItem;

import com.burak.dream_shops.model.CartItem;

public interface ICartItemService {
    void addItemToCart(Long cartId,Long productId , int quantity);
    void removeItemFromCart(Long cartId,Long productId );
    void updateItemQuantity(Long cartId,Long productId , int quantity);
    CartItem getCartItem(Long cartId, Long productId);

}
