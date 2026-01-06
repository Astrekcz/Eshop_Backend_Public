// src/main/java/org/example/zeniqbackend/dto/image/CreateImageRequestDTO.java
package org.example.eshopbackend.dto.image;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateImageRequestDTO {
    @URL @NotBlank
    private String url;

    private String altText;

    /** volitelně – když neuvedeš, nastaví se na konec */
    private Integer sortOrder;

    /** volitelně – pokud true, udělá z něj hlavní a ostatní zruší */
    private Boolean primary;
}
