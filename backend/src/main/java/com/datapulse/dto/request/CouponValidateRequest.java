package com.datapulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponValidateRequest {
    @NotBlank
    private String code;

    @NotNull
    private Double subtotal;
}
