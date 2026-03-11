package com.burak.dream_shops.dto;
import com.burak.dream_shops.model.Product;

import java.math.BigDecimal;

public class CartItemDto {
    private Long itemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private ProductDto product;
}