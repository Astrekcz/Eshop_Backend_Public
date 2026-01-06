// src/main/java/org/example/zeniqbackend/controllers/CatalogController.java
package org.example.eshopbackend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.dto.CategoryResponseDTO;
import org.example.eshopbackend.dto.ProductResponseDTO;
import org.example.eshopbackend.dto.image.ImageResponseDTO;
import org.example.eshopbackend.mapper.CategoryMapper;
import org.example.eshopbackend.service.CatalogService;
import org.example.eshopbackend.service.ImageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalog;
    private final CategoryMapper categoryMapper;
    private final ImageService imageService;

    // --- Produkty (veřejný výpis s primaryImageUrl) ---
    @GetMapping("/products")
    public Page<ProductResponseDTO> list(
            @PageableDefault(size = 12, sort = "productName", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return catalog.listDto(pageable);
    }

    @GetMapping("/products/{id}")
    public ProductResponseDTO byId(@PathVariable Long id) {
        return catalog.getDtoById(id);
    }

    @GetMapping("/products/slug/{slug}")
    public ProductResponseDTO bySlug(@PathVariable String slug) {
        return catalog.getDtoBySlug(slug);
    }

    // --- Vyhledávání ---
    @GetMapping("/products/search")
    public Page<ProductResponseDTO> search(
            @RequestParam("q") String q,
            @PageableDefault(size = 12, sort = "productName", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return catalog.searchDto(q, pageable);
    }

    // --- Kategorie (navbar) ---
    @GetMapping("/categories")
    public List<CategoryResponseDTO> categories() {
        return catalog.listCategories()
                .stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @GetMapping("/categories/{slug}/products")
    public Page<ProductResponseDTO> productsByCategory(
            @PathVariable String slug,
            @PageableDefault(size = 12, sort = "productName", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return catalog.listDtoByCategorySlug(slug, pageable);
    }

    // CatalogController
    @GetMapping("/products/{id}/images")
    public List<ImageResponseDTO> imagesPublic(@PathVariable Long id) {
        return imageService.listByProduct(id); // vrať jen public fotky
    }

}
