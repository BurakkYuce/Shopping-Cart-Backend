package com.datapulse.dto.response;

import com.datapulse.model.Product;
import lombok.Data;

@Data
public class ProductResponse {
    private String id;
    private String storeId;
    private String categoryId;
    private String sku;
    private String name;
    private Double unitPrice;
    private String description;
    private String imageUrl;
    private Integer stockQuantity;

    public static ProductResponse from(Product product) {
        ProductResponse r = new ProductResponse();
        r.id = product.getId();
        r.storeId = product.getStoreId();
        r.categoryId = product.getCategoryId();
        r.sku = product.getSku();
        r.name = product.getName();
        r.unitPrice = product.getUnitPrice();
        r.description = product.getDescription();
        r.imageUrl = product.getImageUrl();
        r.stockQuantity = product.getStockQuantity();
        return r;
    }
}
