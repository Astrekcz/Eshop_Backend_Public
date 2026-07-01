package org.example.eshopbackend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {
    private Long id;
    private String categoryName;
    private String slug;
}
