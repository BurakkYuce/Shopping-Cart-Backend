package com.datapulse.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReviewRequest {
    @NotBlank
    private String productId;
    @Min(1) @Max(5)
    private int starRating;
    private String reviewHeadline;
    private String reviewText;
}
