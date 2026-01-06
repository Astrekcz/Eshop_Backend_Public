// src/main/java/org/example/zeniqbackend/service/ImageService.java
package org.example.eshopbackend.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.image.CreateImageRequestDTO;
import org.example.eshopbackend.dto.image.ImageResponseDTO;
import org.example.eshopbackend.dto.image.UpdateImageRequestDTO;
import org.example.eshopbackend.entity.Image;
import org.example.eshopbackend.entity.Product;
import org.example.eshopbackend.exception.NotFoundException;
import org.example.eshopbackend.mapper.ImageMapper;
import org.example.eshopbackend.repository.ImageRepository;
import org.example.eshopbackend.repository.ProductRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ImageMapper imageMapper;

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
    }

    private Image findImageOrThrow(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image not found: " + imageId));
    }

    @Transactional(readOnly = true)
    public List<ImageResponseDTO> listByProduct(Long productId) {
        Product product = findProductOrThrow(productId);
        return imageRepository.findByProductOrderBySortOrderAscImageIdAsc(product)
                .stream().map(imageMapper::toDto).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ImageResponseDTO addToProduct(Long productId, @Valid CreateImageRequestDTO dto) {
        Product product = findProductOrThrow(productId);

        int defaultOrder = imageRepository.countByProduct(product); // konec seznamu
        int sortOrder = dto.getSortOrder() != null ? dto.getSortOrder() : defaultOrder;

        boolean firstImage = defaultOrder == 0;
        boolean makePrimary = Boolean.TRUE.equals(dto.getPrimary()) || firstImage;

        if (makePrimary) {
            // zruš primární na ostatních
            imageRepository.findByProductOrderBySortOrderAscImageIdAsc(product).forEach(img -> {
                if (img.isPrimary()) img.setPrimary(false);
            });
        }

        Image image = Image.builder()
                .product(product)
                .url(dto.getUrl())
                .altText(dto.getAltText())
                .sortOrder(sortOrder)
                .primary(makePrimary)
                .build();

        // jednoduché přečíslování, pokud se vkládá doprostřed
        imageRepository.findByProductOrderBySortOrderAscImageIdAsc(product).forEach(img -> {
            if (img.getSortOrder() >= sortOrder) {
                img.setSortOrder(img.getSortOrder() + 1);
            }
        });

        Image saved = imageRepository.save(image);
        return imageMapper.toDto(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ImageResponseDTO update(Long imageId, @Valid UpdateImageRequestDTO dto) {
        Image image = findImageOrThrow(imageId);
        Product product = image.getProduct();

        if (dto.getUrl() != null) image.setUrl(dto.getUrl());
        if (dto.getAltText() != null) image.setAltText(dto.getAltText());

        if (dto.getSortOrder() != null) {
            int newOrder = dto.getSortOrder();
            int oldOrder = image.getSortOrder();
            if (newOrder != oldOrder) {
                // posun ostatních
                List<Image> all = imageRepository.findByProductOrderBySortOrderAscImageIdAsc(product);
                if (newOrder < oldOrder) {
                    // posuneme dolů ty mezi <newOrder, oldOrder)
                    for (Image img : all) {
                        if (!img.getImageId().equals(image.getImageId())
                                && img.getSortOrder() >= newOrder && img.getSortOrder() < oldOrder) {
                            img.setSortOrder(img.getSortOrder() + 1);
                        }
                    }
                } else {
                    // posuneme nahoru ty mezi (oldOrder, newOrder]
                    for (Image img : all) {
                        if (!img.getImageId().equals(image.getImageId())
                                && img.getSortOrder() <= newOrder && img.getSortOrder() > oldOrder) {
                            img.setSortOrder(img.getSortOrder() - 1);
                        }
                    }
                }
                image.setSortOrder(newOrder);
            }
        }

        if (dto.getPrimary() != null) {
            boolean makePrimary = dto.getPrimary();
            if (makePrimary) {
                // zruš primární ostatním
                imageRepository.findByProductOrderBySortOrderAscImageIdAsc(product).forEach(img -> img.setPrimary(false));
                image.setPrimary(true);
            } else if (image.isPrimary() && !makePrimary) {
                // nesmíš nechat produkt bez primárního – buď necháme, nebo nastavíme jiný jako primary
                image.setPrimary(false);
                imageRepository.findByProductOrderBySortOrderAscImageIdAsc(product).stream()
                        .filter(img -> !img.getImageId().equals(image.getImageId()))
                        .findFirst()
                        .ifPresent(img -> img.setPrimary(true));
            }
        }

        Image saved = imageRepository.save(image);
        return imageMapper.toDto(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long imageId) {
        Image image = findImageOrThrow(imageId);
        Product product = image.getProduct();
        boolean wasPrimary = image.isPrimary();
        int removedOrder = image.getSortOrder();

        imageRepository.delete(image);

        // zkompaktovat pořadí
        List<Image> rest = imageRepository.findByProductOrderBySortOrderAscImageIdAsc(product);
        for (Image img : rest) {
            if (img.getSortOrder() > removedOrder) {
                img.setSortOrder(img.getSortOrder() - 1);
            }
        }

        // zajisti, že nějaký primary zůstane
        if (wasPrimary && !rest.isEmpty() && rest.stream().noneMatch(Image::isPrimary)) {
            rest.get(0).setPrimary(true);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ImageResponseDTO setPrimary(Long imageId) {
        Image image = findImageOrThrow(imageId);
        Product product = image.getProduct();

        imageRepository.findByProductOrderBySortOrderAscImageIdAsc(product)
                .forEach(img -> img.setPrimary(false));
        image.setPrimary(true);

        return imageMapper.toDto(imageRepository.save(image));
    }
}
