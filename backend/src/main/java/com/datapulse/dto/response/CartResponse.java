package com.datapulse.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class CartResponse {
    private List<CartItemResponse> items;
    private int totalItems;
    private Double totalPrice;
}
