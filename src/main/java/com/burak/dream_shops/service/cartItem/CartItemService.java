package com.burak.dream_shops.service.cartItem;

import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Cart;
import com.burak.dream_shops.model.CartItem;
import com.burak.dream_shops.model.Product;
import com.burak.dream_shops.repository.CartItemRepository.CartItemRepository;
import com.burak.dream_shops.repository.CartRepository.CartRepository;
import com.burak.dream_shops.service.cart.ICartService;
import com.burak.dream_shops.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Service
@RequiredArgsConstructor
public class CartItemService implements ICartItemService {
    private final CartItemRepository cartItemRepository;
    private final IProductService iProductService;
    private final ICartService cartService;
    private final CartRepository cartRepository;
    @Override
    public void addItemToCart(Long cartId, Long productId, int quantity) {
    /*
    * 1- Get the cart
    * 2- Get the product
    * 3- Check if the product already in the cart
    * 4- If yes , then increase the quantity with the requested quantity
    * 5- If no , the initiate a new CartItem entry
    * */
    Cart cart= cartService.getCart(cartId);
    Product product=iProductService.getProductById(productId);
    CartItem cartItem=cart.getItems()
            .stream()
            .filter(item->item.getProduct().getId().equals(productId))
            .findFirst().orElse(null);
    if (cartItem.getId()==null){
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setUnitPrice(product.getPrice());
    }else {
        cartItem.setQuantity(cartItem.getQuantity()+quantity);
    }
    cartItem.setTotalPrice();
    cart.addItem(cartItem);
    cartItemRepository.save(cartItem);
    cartRepository.save(cart);
    }

    @Override
    public void removeItemFromCart(Long cartId, Long productId) {
    Cart cart=cartService.getCart(cartId);
    CartItem itemToRemove =getCartItem(cartId,productId);
    cart.removeItem(itemToRemove);
    cartRepository.save(cart);
    }

    @Override
    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        Cart cart=cartService.getCart(cartId);
        cart.getItems().stream().filter(item->item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item->{
                    item.setQuantity(quantity);
                    item.setUnitPrice(item.getProduct().getPrice());
                    item.setTotalPrice();
                });
        BigDecimal totalAmount=cart.getTotalAmount();
        cart.setTotalAmount(totalAmount);
        cartRepository.save(cart);
    }
@Override
public CartItem getCartItem(Long cartId,Long productId){
        Cart cart = cartService.getCart(cartId);
        return cart.getItems()
            .stream()
            .filter(item->item.getProduct().getId().equals(productId))
            .findFirst().orElseThrow(()->new ResourcesNotFoundException("Product not found"));
}


}
