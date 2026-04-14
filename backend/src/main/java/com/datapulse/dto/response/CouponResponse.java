package com.datapulse.dto.response;

import com.datapulse.model.Coupon;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponResponse {
    private String id;
    private String code;
    private String type;
    private Double value;
    private String description;
    private Double minOrderAmount;
    private Double maxDiscount;
    private Integer maxUses;
    private Integer currentUses;
    private Boolean active;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime createdAt;

    public static CouponResponse from(Coupon c) {
        CouponResponse r = new CouponResponse();
        r.setId(c.getId());
        r.setCode(c.getCode());
        r.setType(c.getType());
        r.setValue(c.getValue());
        r.setDescription(c.getDescription());
        r.setMinOrderAmount(c.getMinOrderAmount());
        r.setMaxDiscount(c.getMaxDiscount());
        r.setMaxUses(c.getMaxUses());
        r.setCurrentUses(c.getCurrentUses());
        r.setActive(c.getActive());
        r.setValidFrom(c.getValidFrom());
        r.setValidTo(c.getValidTo());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }
}
