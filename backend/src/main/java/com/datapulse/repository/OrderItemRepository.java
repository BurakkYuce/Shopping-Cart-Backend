package com.datapulse.repository;

import com.datapulse.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    List<OrderItem> findByOrderId(String orderId);
    List<OrderItem> findByOrderIdIn(List<String> orderIds);
    List<OrderItem> findByProductIdIn(List<String> productIds);
    void deleteByProductId(String productId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                WHERE o.user_id = :userId
                  AND oi.product_id = :productId
                  AND o.status = 'delivered'
            )
            """, nativeQuery = true)
    boolean existsDeliveredOrderWithProduct(@Param("userId") String userId,
                                            @Param("productId") String productId);
}
