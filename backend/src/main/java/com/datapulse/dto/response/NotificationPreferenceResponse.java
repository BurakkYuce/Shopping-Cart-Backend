package com.datapulse.dto.response;

import com.datapulse.model.NotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {
    private boolean orderUpdates;
    private boolean newArrivals;
    private boolean promotions;
    private boolean newsletter;
    // seller flags
    private boolean newOrderSeller;
    private boolean lowStockAlert;
    private boolean newReviewAlert;
    private boolean weeklyStoreDigest;

    public static NotificationPreferenceResponse from(NotificationPreference p) {
        return NotificationPreferenceResponse.builder()
                .orderUpdates(Boolean.TRUE.equals(p.getOrderUpdates()))
                .newArrivals(Boolean.TRUE.equals(p.getNewArrivals()))
                .promotions(Boolean.TRUE.equals(p.getPromotions()))
                .newsletter(Boolean.TRUE.equals(p.getNewsletter()))
                .newOrderSeller(Boolean.TRUE.equals(p.getNewOrderSeller()))
                .lowStockAlert(Boolean.TRUE.equals(p.getLowStockAlert()))
                .newReviewAlert(Boolean.TRUE.equals(p.getNewReviewAlert()))
                .weeklyStoreDigest(Boolean.TRUE.equals(p.getWeeklyStoreDigest()))
                .build();
    }
}
