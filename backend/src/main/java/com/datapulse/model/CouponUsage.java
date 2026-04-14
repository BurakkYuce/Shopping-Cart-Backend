package com.datapulse.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_usages", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"coupon_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsage {

    @Id
    private String id;

    @Column(name = "coupon_id", nullable = false)
    private String couponId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
