package com.datapulse.dto.request;

import lombok.Data;

@Data
public class UpdateNotificationPreferenceRequest {
    private Boolean orderUpdates;
    private Boolean newArrivals;
    private Boolean promotions;
    private Boolean newsletter;
    private Boolean newOrderSeller;
    private Boolean lowStockAlert;
    private Boolean newReviewAlert;
    private Boolean weeklyStoreDigest;
}
