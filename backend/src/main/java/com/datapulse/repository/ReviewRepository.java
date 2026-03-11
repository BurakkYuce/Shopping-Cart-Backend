package com.datapulse.repository;

import com.datapulse.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByProductId(String productId);
    List<Review> findByProductIdIn(List<String> productIds);
    List<Review> findByUserId(String userId);
}
