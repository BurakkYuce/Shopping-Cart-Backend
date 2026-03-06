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

import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
/**
 * Sepet kalemlerini (CartItem) yöneten servis sınıfı.
 * Sepete ürün ekleme, çıkarma ve miktar güncelleme işlemlerini gerçekleştirir.
 */
@Service
@RequiredArgsConstructor
public class CartItemService implements ICartItemService {
    private final CartItemRepository cartItemRepository;
    private final IProductService iProductService;
    private final ICartService cartService;
    private final CartRepository cartRepository;

    /**
     * Sepete ürün ekler.
     * Algoritma:
     * 1. Sepeti getir (cartId ile)
     * 2. Ürünü getir (productId ile)
     * 3. Bu ürün sepette zaten var mı kontrol et
     * 4. Varsa: mevcut miktara istenen miktarı ekle
     * 5. Yoksa: yeni bir CartItem oluştur, fiyatını ve miktarını set et
     * Son olarak toplam fiyatı hesapla ve kaydet.
     */
    @Override
    @Transactional
    public void addItemToCart(Long cartId, Long productId, int quantity) {
        Cart cart = cartService.getCart(cartId);
        Product product = iProductService.getProductById(productId);
        // Ürün sepette var mı? Varsa onu al, yoksa yeni boş CartItem oluştur
        CartItem cartItem = cart.getItems()
                .stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst().orElse(new CartItem());
        if (cartItem.getId() == null) {
            // Yeni ürün: sepet, ürün, miktar ve birim fiyatı set et
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setUnitPrice(product.getPrice());
        } else {
            // Zaten var: sadece miktarı artır
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }
        cartItem.setTotalPrice(); // unitPrice * quantity hesaplar
        cart.addItem(cartItem);
        cartItemRepository.save(cartItem);
        cartRepository.save(cart);
    }

    /**
     * Sepetten belirli bir ürünü kaldırır.
     * Önce sepetin kendisini, sonra kaldırılacak CartItem'ı bulur ve siler.
     * Sepet totalAmount'u otomatik güncellenir (Cart.removeItem içinde).
     */
    @Override
    @Transactional
    public void removeItemFromCart(Long cartId, Long productId) {
        Cart cart = cartService.getCart(cartId);
        CartItem itemToRemove = getCartItem(cartId, productId);
        cart.removeItem(itemToRemove);
        cartRepository.save(cart);
    }

    /**
     * Sepetteki bir ürünün miktarını günceller.
     * Ürünün birim fiyatını tekrar ürün tablosundan çeker (fiyat değişmiş olabilir),
     * totalPrice'ı yeniden hesaplar ve tüm sepet toplamını günceller.
     */
    @Override
    @Transactional
    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        Cart cart = cartService.getCart(cartId);
        cart.getItems().stream().filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(quantity);
                    item.setUnitPrice(item.getProduct().getPrice());
                    item.setTotalPrice();
                });
        // Tüm kalemlerin toplamını hesapla
        BigDecimal totalAmount = cart.getItems()
                .stream().map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(totalAmount);
        cartRepository.save(cart);
    }

    /**
     * Sepetteki belirli bir CartItem'ı productId'ye göre bulur ve döner.
     * Bulunamazsa ResourcesNotFoundException fırlatır.
     */
    @Override
    public CartItem getCartItem(Long cartId, Long productId) {
        Cart cart = cartService.getCart(cartId);
        return cart.getItems()
                .stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst().orElseThrow(() -> new ResourcesNotFoundException("Product not found"));
    }
}
