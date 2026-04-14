package com.datapulse.dto.response;

import com.datapulse.model.Product;
import com.datapulse.model.ProductAttribute;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private String brand;
    private String brandId;
    private Double rating;
    private Integer reviewCount;
    private Double retailPrice;
    private Map<String, String> attributes;

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
        r.brand = product.getBrand();
        r.brandId = product.getBrandId();
        r.rating = product.getRating();
        r.reviewCount = 0;
        r.retailPrice = product.getRetailPrice();
        return r;
    }

    /** Apply user-generated review aggregates. If reviewCount is 0, rating is cleared so the
     *  frontend can render "no reviews yet" rather than a seeded rating. */
    public ProductResponse withReviewAggregates(Double avgRating, int reviewCount) {
        this.reviewCount = reviewCount;
        this.rating = reviewCount > 0 ? avgRating : null;
        return this;
    }

    public static ProductResponse from(Product product, List<ProductAttribute> attrs) {
        ProductResponse r = from(product);
        if (attrs != null && !attrs.isEmpty()) {
            r.attributes = attrs.stream()
                    .collect(Collectors.toMap(ProductAttribute::getAttrKey, a -> a.getAttrValue() != null ? a.getAttrValue() : ""));
        }
        return r;
    }
}
