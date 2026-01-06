package org.example.eshopbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.*;
import org.example.eshopbackend.entity.Role;

import java.time.LocalDate;

@Getter
@Setter
public class CreateUserRequestDTO {
    @NotNull
    private String firstName;
    @NotNull
    private String lastName;
    @NotNull
    private String password;
    @NotNull
    private String email;
    @NotNull
    private String phoneNumber;


    @NotNull
    @Past(message = "Datum narození musí být v minulosti")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Role role;  //TODO defaultne musi byt uzivatel s pravama USER

}
