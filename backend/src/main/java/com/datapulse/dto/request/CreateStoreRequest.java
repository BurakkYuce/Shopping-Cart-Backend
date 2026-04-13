package com.datapulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateStoreRequest {
    @NotBlank
    private String name;
    private String description;
    private String address;
    private String city;
    private String phone;
    private String logoUrl;
}
