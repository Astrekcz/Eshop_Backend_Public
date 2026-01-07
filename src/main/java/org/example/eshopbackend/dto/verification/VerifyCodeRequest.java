package org.example.eshopbackend.dto.verification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank(message = "Kód je povinný")
    private String code;
}