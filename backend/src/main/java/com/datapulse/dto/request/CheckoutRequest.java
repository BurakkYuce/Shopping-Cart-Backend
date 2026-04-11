package com.datapulse.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotBlank
    private String storeId;

    @NotBlank
    private String paymentMethod;

    @NotNull(message = "KVKK consent is required")
    @AssertTrue(message = "KVKK consent is required")
    private Boolean kvkkConsent;

    @NotNull(message = "Distance sale contract acceptance is required")
    @AssertTrue(message = "Distance sale contract acceptance is required")
    private Boolean distanceSaleConsent;

    @NotNull(message = "Pre-information acknowledgement is required")
    @AssertTrue(message = "Pre-information acknowledgement is required")
    private Boolean preInformationConsent;
}
