// src/main/java/org/example/zeniqbackend/dto/image/UpdateImageRequestDTO.java
package org.example.eshopbackend.dto.image;

import lombok.*;
import org.hibernate.validator.constraints.URL;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateImageRequestDTO {
    @URL
    private String url;

    private String altText;

    private Integer sortOrder;

    private Boolean primary;
}
