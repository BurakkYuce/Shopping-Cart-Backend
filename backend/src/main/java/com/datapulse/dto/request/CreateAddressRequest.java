package com.datapulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAddressRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String fullName;
    private String phone;
    @NotBlank
    private String addressLine1;
    private String addressLine2;
    @NotBlank
    private String city;
    private String district;
    private String zipCode;
    private String country = "Turkey";
    private Boolean isDefault = false;
}
