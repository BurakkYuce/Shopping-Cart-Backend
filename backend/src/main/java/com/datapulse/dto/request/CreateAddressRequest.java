package com.datapulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAddressRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String fullName;
    @Pattern(regexp = "^(\\+90|0)?5\\d{9}$", message = "Invalid TR phone number (e.g. 05XXXXXXXXX)")
    private String phone;
    @NotBlank
    private String addressLine1;
    private String addressLine2;
    @NotBlank
    private String city;
    private String district;
    @Pattern(regexp = "^\\d{5}$", message = "Zip code must be 5 digits")
    private String zipCode;
    private String country = "Turkey";
    private Boolean isDefault = false;
}
