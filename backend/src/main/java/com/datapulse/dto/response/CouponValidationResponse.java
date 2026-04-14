package com.datapulse.dto.response;

import lombok.Data;

@Data
public class CouponValidationResponse {
    private boolean valid;
    private String code;
    private String type;
    private double discountAmount;
    private String description;
    private String message;
}
