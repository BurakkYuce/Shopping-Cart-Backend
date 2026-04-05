package com.datapulse.dto.response;

import lombok.Data;

@Data
public class CartItemResponse {
    private String id;
    private String productId;
    private String productName;
    private Double unitPrice;
    private Integer quantity;
    private Double lineTotal;
}
