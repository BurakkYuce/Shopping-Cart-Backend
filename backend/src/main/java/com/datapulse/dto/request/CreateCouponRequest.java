package com.datapulse.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateCouponRequest {
    private String code;
    private String type;
    private Double value;
    private String description;
    private Double minOrderAmount;
    private Double maxDiscount;
    private Integer maxUses;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
