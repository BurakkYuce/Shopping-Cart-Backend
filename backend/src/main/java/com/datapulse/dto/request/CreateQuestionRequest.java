package com.datapulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateQuestionRequest {
    @NotBlank
    private String productId;

    @NotBlank
    @Size(max = 2000)
    private String question;
}
