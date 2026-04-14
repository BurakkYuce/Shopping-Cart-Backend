package com.datapulse.dto.response;

import lombok.Data;

@Data
public class CartItemResponse {
    private String id;
    private String productId;
    private String productName;
    private String imageUrl;
    private Double unitPrice;
    private Integer quantity;
    private Double lineTotal;
    private String storeId;
    private String storeName;
}
