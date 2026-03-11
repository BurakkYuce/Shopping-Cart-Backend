package com.datapulse.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class AnalyticsSalesResponse {
    private Double totalRevenue;
    private Long orderCount;
    private Double averageOrderValue;
    private Map<String, Double> revenueByDay;
    private String fromDate;
    private String toDate;
}
