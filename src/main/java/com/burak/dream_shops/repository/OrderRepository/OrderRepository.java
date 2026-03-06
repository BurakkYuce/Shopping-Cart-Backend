package com.burak.dream_shops.repository.OrderRepository;

import com.burak.dream_shops.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order>findByUserId(Long userId);
}
