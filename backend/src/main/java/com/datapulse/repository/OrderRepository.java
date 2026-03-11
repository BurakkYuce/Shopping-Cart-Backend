package com.datapulse.repository;

import com.datapulse.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
