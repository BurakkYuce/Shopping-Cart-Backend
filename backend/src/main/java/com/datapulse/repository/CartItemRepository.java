package com.datapulse.repository;

import com.datapulse.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findByUserId(String userId);
    Optional<CartItem> findByUserIdAndProductId(String userId, String productId);
    void deleteByUserId(String userId);
    void deleteByUserIdAndProductId(String userId, String productId);
    void deleteByProductId(String productId);

    @Modifying
    @Query(value = "INSERT INTO cart_items (id, user_id, product_id, quantity) " +
            "VALUES (:id, :userId, :productId, :quantity) " +
            "ON CONFLICT (user_id, product_id) DO UPDATE " +
            "SET quantity = cart_items.quantity + EXCLUDED.quantity",
            nativeQuery = true)
    void upsertCartItem(@Param("id") String id,
                        @Param("userId") String userId,
                        @Param("productId") String productId,
                        @Param("quantity") Integer quantity);
}
