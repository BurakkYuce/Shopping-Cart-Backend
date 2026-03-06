package com.burak.dream_shops.service.cart;

import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Cart;
import com.burak.dream_shops.model.CartItem;
import com.burak.dream_shops.repository.CartItemRepository.CartItemRepository;
import com.burak.dream_shops.repository.CartRepository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sepet (Cart) işlemlerini yöneten servis sınıfı.
 * Sepet alma, temizleme, toplam fiyat hesaplama ve yeni sepet oluşturma işlemlerini gerçekleştirir.
 */
@Service
@RequiredArgsConstructor
public class CartService implements ICartService{
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AtomicLong cartIdGenerator = new AtomicLong(0);

    /**
     * Verilen ID'ye sahip sepeti getirir.
     * Sepetteki tüm ürünlerin fiyatlarını toplayarak totalAmount alanını günceller
     * ve güncellenmiş haliyle kaydeder.
     */
    @Override
    @Transactional
    public Cart getCart(Long id) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Cart not found"));
        BigDecimal totalAmount = cart.getItems().stream().map(CartItem::getTotalPrice).reduce(BigDecimal.ZERO,BigDecimal::add);
        cart.setTotalAmount(totalAmount);
        return cartRepository.save(cart);
    }

    /**
     * Sepeti tamamen temizler: önce sepetteki tüm ürünleri (CartItem) siler,
     * ardından Cart kaydını da veritabanından kaldırır.
     */
    @Transactional
    @Override
    public void clearCart(Long id) {
        Cart cart = getCart(id);
        cartItemRepository.deleteAllByCartId(id);
        cart.getItems().clear();
        cartRepository.deleteById(id);
    }

    /**
     * Sepette kayıtlı toplam tutarı döner.
     * Doğrudan DB'den okur, hesaplama yapmaz.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalPrice(Long id) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Cart not found"));
        return cart.getTotalAmount();
    }

    /**
     * Boş bir yeni Cart oluşturur ve veritabanına kaydeder.
     * Oluşturulan sepetin ID'sini döner; bu ID CartItem eklerken kullanılır.
     */
    @Override
    public Long initializeNewCart() {
        Cart newCart = new Cart();
        return cartRepository.save(newCart).getId();
    }

    /**
     * Kullanıcı ID'sine göre o kullanıcıya ait sepeti getirir.
     * Sipariş oluştururken kullanıcının aktif sepetine ulaşmak için kullanılır.
     */
    @Override
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }
}