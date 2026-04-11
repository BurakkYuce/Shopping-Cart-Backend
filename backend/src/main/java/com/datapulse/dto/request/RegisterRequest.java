package com.datapulse.dto.request;

import com.datapulse.model.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    @NotNull
    private RoleType roleType;
    private String gender;
}
