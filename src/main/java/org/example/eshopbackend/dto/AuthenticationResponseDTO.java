package org.example.eshopbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationResponseDTO {
    private String jwtToken;

    public AuthenticationResponseDTO(String jwtToken) {
        this.jwtToken = jwtToken;
    }

}
