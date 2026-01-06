package org.example.eshopbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryRequestDTO {

    @NotBlank
    private String categoryName;

    @NotBlank
    private String slug;
}
