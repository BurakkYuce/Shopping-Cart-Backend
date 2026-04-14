package com.datapulse.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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

    private String couponCode;
    private Double discountAmount;
    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "KVKK consent is required")
    @AssertTrue(message = "KVKK consent is required")
    private Boolean kvkkConsent;

    @NotNull(message = "Distance sale contract acceptance is required")
    @AssertTrue(message = "Distance sale contract acceptance is required")
    private Boolean distanceSaleConsent;

    @NotNull(message = "Pre-information acknowledgement is required")
    @AssertTrue(message = "Pre-information acknowledgement is required")
    private Boolean preInformationConsent;

    @Data
    public static class OrderItemRequest {
        @NotBlank
        private String productId;
        @NotNull
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;
    }
}
