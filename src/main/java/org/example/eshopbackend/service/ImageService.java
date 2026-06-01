package org.example.eshopbackend.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.image.CreateImageRequestDTO;
import org.example.eshopbackend.dto.image.ImageResponseDTO;
import org.example.eshopbackend.dto.image.UpdateImageRequestDTO;
import org.example.eshopbackend.entity.Image;
import org.example.eshopbackend.entity.Item; // 1. Nová entita
import org.example.eshopbackend.exception.NotFoundException;
import org.example.eshopbackend.mapper.ImageMapper;
import org.example.eshopbackend.repository.ImageRepository;
import org.example.eshopbackend.repository.ItemRepository; // 2. Nový repozitář
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final ItemRepository itemRepository; // Změněno z ProductRepository
    private final ImageMapper imageMapper;

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
    }

    private Image findImageOrThrow(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image not found: " + imageId));
    }

    @Transactional(readOnly = true)
    public List<ImageResponseDTO> listByItem(Long itemId) { // Přejmenováno na listByItem
        Item item = findItemOrThrow(itemId);
        return imageRepository.findByItemOrderBySortOrderAscIdAsc(item)
                .stream().map(imageMapper::toDto).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ImageResponseDTO addToItem(Long itemId, @Valid CreateImageRequestDTO dto) { // Přejmenováno na addToItem
        Item item = findItemOrThrow(itemId);

        int defaultOrder = imageRepository.countByItem(item); // konec seznamu
        int sortOrder = dto.getSortOrder() != null ? dto.getSortOrder() : defaultOrder;

        boolean firstImage = defaultOrder == 0;
        boolean makePrimary = Boolean.TRUE.equals(dto.getPrimary()) || firstImage;

        if (makePrimary) {
            // zruš primární na ostatních
            imageRepository.findByItemOrderBySortOrderAscIdAsc(item).forEach(img -> {
                if (img.isPrimary()) img.setPrimary(false);
            });
        }

        Image image = Image.builder()
                .item(item) // Navázáno na Item místo Product
                .url(dto.getUrl())
                .altText(dto.getAltText())
                .sortOrder(sortOrder)
                .primary(makePrimary)
                .build();

        // jednoduché přečíslování, pokud se vkládá doprostřed
        imageRepository.findByItemOrderBySortOrderAscIdAsc(item).forEach(img -> {
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
        Item item = image.getItem(); // Změněno z g/setProduct

        if (dto.getUrl() != null) image.setUrl(dto.getUrl());
        if (dto.getAltText() != null) image.setAltText(dto.getAltText());

        if (dto.getSortOrder() != null) {
            int newOrder = dto.getSortOrder();
            int oldOrder = image.getSortOrder();
            if (newOrder != oldOrder) {
                // posun ostatních
                List<Image> all = imageRepository.findByItemOrderBySortOrderAscIdAsc(item);
                if (newOrder < oldOrder) {
                    // posuneme dolů ty mezi <newOrder, oldOrder)
                    for (Image img : all) {
                        if (!img.getId().equals(image.getId())
                                && img.getSortOrder() >= newOrder && img.getSortOrder() < oldOrder) {
                            img.setSortOrder(img.getSortOrder() + 1);
                        }
                    }
                } else {
                    // posuneme nahoru ty mezi (oldOrder, newOrder]
                    for (Image img : all) {
                        if (!img.getId().equals(image.getId())
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
                imageRepository.findByItemOrderBySortOrderAscIdAsc(item).forEach(img -> img.setPrimary(false));
                image.setPrimary(true);
            } else if (image.isPrimary() && !makePrimary) {
                // nesmíš nechat produkt bez primárního
                image.setPrimary(false);
                imageRepository.findByItemOrderBySortOrderAscIdAsc(item).stream()
                        .filter(img -> !img.getId().equals(image.getId()))
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
        Item item = image.getItem();
        boolean wasPrimary = image.isPrimary();
        int removedOrder = image.getSortOrder();

        imageRepository.delete(image);

        // zkompaktovat pořadí
        List<Image> rest = imageRepository.findByItemOrderBySortOrderAscIdAsc(item);
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
        Item item = image.getItem();

        imageRepository.findByItemOrderBySortOrderAscIdAsc(item)
                .forEach(img -> img.setPrimary(false));
        image.setPrimary(true);

        return imageMapper.toDto(imageRepository.save(image));
    }
}