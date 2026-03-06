package com.burak.dream_shops.request;

import com.burak.dream_shops.model.Category;
import com.burak.dream_shops.model.Image;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class ProductUpdateRequest {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;
    private String description;
    private Category category;
    private List<Image> images;

}
