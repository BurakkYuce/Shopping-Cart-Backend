package com.datapulse.repository;

import com.datapulse.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, String> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findAllByActiveTrue();
}
