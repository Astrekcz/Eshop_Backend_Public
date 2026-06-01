package org.example.eshopbackend.dto.image;

import lombok.*;

import java.time.LocalDateTime;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImageResponseDTO {
    private Long imageId;
    private Long productId;
    private String url;
    private String altText;
    private boolean primary;
    private int sortOrder;
    private LocalDateTime createdAt;
}
