package com.datapulse.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String storeId;
    @NotBlank
    private String paymentMethod;
    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotBlank
        private String productId;
        @NotNull
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;
    }
}
