package com.datapulse.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class AnalyticsSalesResponse {
    private Double totalRevenue;
    private Long orderCount;
    private Double averageOrderValue;
    private Map<String, Double> revenueByDay;
    private Map<String, Double> revenueByWeek;
    private Map<String, Double> revenueByMonth;
    private Map<String, Double> revenueByCategory;
    private String fromDate;
    private String toDate;
}
