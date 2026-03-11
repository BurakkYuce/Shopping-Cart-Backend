package com.datapulse.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AnalyticsProductResponse {
    private List<TopProduct> topSellingProducts;
    private Map<String, Double> avgRatingByCategory;

    @Data
    public static class TopProduct {
        private String productId;
        private String productName;
        private Long totalQuantity;
        private Double totalRevenue;
    }
}
