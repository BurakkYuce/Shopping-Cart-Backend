package com.datapulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateProductRequest {
    @NotBlank
    private String storeId;
    private String categoryId;
    private String sku;
    @NotBlank
    private String name;
    @NotNull @Positive
    private Double unitPrice;
    private String description;
}
