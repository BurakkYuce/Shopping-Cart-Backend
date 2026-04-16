package com.datapulse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private User user;

    @Column(name = "order_updates", nullable = false)
    private Boolean orderUpdates = true;

    @Column(name = "new_arrivals", nullable = false)
    private Boolean newArrivals = true;

    @Column(name = "promotions", nullable = false)
    private Boolean promotions = false;

    @Column(name = "newsletter", nullable = false)
    private Boolean newsletter = true;

    // Seller-only (CORPORATE) flags. Dispatcher honors them only for store-owner users.
    @Column(name = "new_order_seller", nullable = false)
    private Boolean newOrderSeller = true;

    @Column(name = "low_stock_alert", nullable = false)
    private Boolean lowStockAlert = true;

    @Column(name = "new_review_alert", nullable = false)
    private Boolean newReviewAlert = true;

    @Column(name = "weekly_store_digest", nullable = false)
    private Boolean weeklyStoreDigest = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    @PrePersist
    void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
