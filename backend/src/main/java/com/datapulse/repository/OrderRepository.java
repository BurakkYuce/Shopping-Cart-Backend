package com.datapulse.repository;

import com.datapulse.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);
    Page<Order> findByUserId(String userId, Pageable pageable);
    List<Order> findByStoreId(String storeId);
    List<Order> findByStoreIdIn(List<String> storeIds);
    Page<Order> findByStoreIdIn(List<String> storeIds, Pageable pageable);
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<Order> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime from, LocalDateTime to);
    List<Order> findByStoreIdInAndCreatedAtBetween(List<String> storeIds, LocalDateTime from, LocalDateTime to);

    /** Seller-facing listing that floats action-required orders to the top so nothing gets missed:
     *  PROCESSING (needs shipping) first, then PENDING (needs confirmation), then in-flight SHIPPED,
     *  then terminal states. Within each bucket, newest first. */
    @Query("SELECT o FROM Order o WHERE o.storeId IN :storeIds ORDER BY " +
            "CASE o.status " +
            "  WHEN com.datapulse.model.enums.OrderStatus.PROCESSING THEN 1 " +
            "  WHEN com.datapulse.model.enums.OrderStatus.PENDING THEN 2 " +
            "  WHEN com.datapulse.model.enums.OrderStatus.SHIPPED THEN 3 " +
            "  WHEN com.datapulse.model.enums.OrderStatus.DELIVERED THEN 4 " +
            "  WHEN com.datapulse.model.enums.OrderStatus.RETURNED THEN 5 " +
            "  ELSE 6 " +
            "END ASC, o.createdAt DESC")
    Page<Order> findSellerOrdersByPriority(@Param("storeIds") List<String> storeIds, Pageable pageable);
}
