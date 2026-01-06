// src/main/java/org/example/zeniqbackend/service/CatalogService.java
package org.example.eshopbackend.service;

import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.dto.ProductResponseDTO;
import org.example.eshopbackend.entity.Category;
import org.example.eshopbackend.entity.Product;
import org.example.eshopbackend.mapper.ProductMapper;
import org.example.eshopbackend.repository.CategoryRepository;
import org.example.eshopbackend.repository.ImageRepository;
import org.example.eshopbackend.repository.ProductRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ProductRepository repo;
    private final CategoryRepository catRepo;
    private final ImageRepository imageRepo;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public Page<Product> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Product getBySlug(String slug) {
        return repo.findBySlug(slug).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return repo.findById(id).orElseThrow();
    }

    public List<Category> listCategories() {
        return catRepo.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public Page<Product> listByCategorySlug(String slug, Pageable pageable) {
        return repo.findByCategorySlug(slug, pageable);
    }

    /* ====== DTO helpery s primaryImageUrl ====== */

    private String loadPrimaryImageUrl(Long productId) {
        return imageRepo.findTopUrlsByProductId(productId, PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> listDto(Pageable pageable) {
        return list(pageable).map(p -> {
            ProductResponseDTO dto = productMapper.toDto(p);
            dto.setPrimaryImageUrl(loadPrimaryImageUrl(p.getProductId()));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getDtoBySlug(String slug) {
        Product p = getBySlug(slug);
        ProductResponseDTO dto = productMapper.toDto(p);
        dto.setPrimaryImageUrl(loadPrimaryImageUrl(p.getProductId()));
        return dto;
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getDtoById(Long id) {
        Product p = getById(id);
        ProductResponseDTO dto = productMapper.toDto(p);
        dto.setPrimaryImageUrl(loadPrimaryImageUrl(p.getProductId()));
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> listDtoByCategorySlug(String slug, Pageable pageable) {
        return listByCategorySlug(slug, pageable).map(p -> {
            ProductResponseDTO dto = productMapper.toDto(p);
            dto.setPrimaryImageUrl(loadPrimaryImageUrl(p.getProductId()));
            return dto;
        });
    }
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchDto(String q, Pageable pageable) {
        var page = repo.search(q, pageable);
        return page.map(productMapper::toDto);
    }

}
