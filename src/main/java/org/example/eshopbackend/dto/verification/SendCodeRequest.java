package org.example.eshopbackend.dto.verification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeRequest {
    @NotBlank(message = "Email je povinný")
    @Email(message = "Neplatný formát emailu")
    private String email;
}