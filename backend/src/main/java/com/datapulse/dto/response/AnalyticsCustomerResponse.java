package com.datapulse.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class AnalyticsCustomerResponse {
    private Map<String, Double> spendByMembership;
    private Map<String, Long> satisfactionDistribution;
    private Map<String, Long> topCities;
}
