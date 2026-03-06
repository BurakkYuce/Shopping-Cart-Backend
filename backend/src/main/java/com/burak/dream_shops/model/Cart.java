package com.burak.dream_shops.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@Entity
@NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CartItem> items = new HashSet<>();


    /**
     * Sepete yeni ürün ekler.
     * İki yönlü ilişkiyi kurar: item.cart = this, this.items.add(item).
     * Toplam tutarı günceller.
     */
    public void addItem(CartItem item) {
        this.items.add(item);
        item.setCart(this);
        updateTotalAmount();
    }

    /**
     * Sepetten ürün kaldırır.
     * item.cart = null yaparak ilişkiyi koparır.
     * Toplam tutarı günceller.
     */
    public void removeItem(CartItem item) {
        this.items.remove(item);
        item.setCart(null);
        updateTotalAmount();
    }

    /**
     * Sepetteki tüm kalemlerin unitPrice * quantity toplamını hesaplar
     * ve totalAmount'a yazar. Her ekleme/çıkarmada otomatik çağrılır.
     */
    private void updateTotalAmount() {
        this.totalAmount = items.stream().map(item -> {
            BigDecimal unitPrice = item.getUnitPrice();
            if (unitPrice == null) {
                return BigDecimal.ZERO;
            }
            return unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
