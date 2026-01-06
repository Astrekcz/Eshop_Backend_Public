package org.example.eshopbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserRequestDTO {

    private String firstName;

    private String lastName;

    private String password;

    private String email;

    private String phoneNumber;



    @Past(message = "Datum narození musí být v minulosti")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

}
