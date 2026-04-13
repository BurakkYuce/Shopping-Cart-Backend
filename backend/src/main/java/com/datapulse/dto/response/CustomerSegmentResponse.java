package com.datapulse.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class CustomerSegmentResponse {
    private List<Segment> segments;
    private long totalCustomers;

    @Data
    public static class Segment {
        private String name;
        private long count;
        private double percentage;
        private double avgSpend;
    }
}
