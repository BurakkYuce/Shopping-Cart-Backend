package com.datapulse.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StoreKpiResponse {
    private String storeId;
    private String storeName;
    private double totalRevenue;
    private long orderCount;
    private double averageOrderValue;
    private long customerCount;
    private List<TopProduct> topProducts;
    private Map<Integer, Long> ratingDistribution;

    @Data
    public static class TopProduct {
        private String productId;
        private String productName;
        private long totalQuantity;
        private double totalRevenue;
    }
}
