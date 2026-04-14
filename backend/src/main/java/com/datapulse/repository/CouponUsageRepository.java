package com.datapulse.repository;

import com.datapulse.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, String> {

    boolean existsByCouponIdAndUserId(String couponId, String userId);
}
