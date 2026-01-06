// src/main/java/org/example/zeniqbackend/service/ProductService.java
package org.example.eshopbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.CreateProductRequestDTO;
import org.example.eshopbackend.dto.UpdateProductRequestDTO;
import org.example.eshopbackend.entity.Category;
import org.example.eshopbackend.entity.Product;
import org.example.eshopbackend.mapper.ProductMapper;
import org.example.eshopbackend.repository.CategoryRepository;
import org.example.eshopbackend.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper mapper;

    // CREATE
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product addProduct(CreateProductRequestDTO dto) {
        Category cat = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.getCategoryId()));

        String base = slugify(dto.getProductName());
        String slug = ensureUniqueSlug(base);

        Product entity = mapper.toEntity(dto, cat, slug);

        // ⬇ sanity: null → 0, záporné clampnout
        Integer w = entity.getWeightGrams();
        if (w == null || w < 0) entity.setWeightGrams(0);

        try {
            Product saved = productRepository.save(entity);
            log.info("Created product id={} slug={} weight={}g", saved.getProductId(), saved.getSlug(), saved.getWeightGrams());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Fallback pro souběh (unikátní slug)
            slug = ensureUniqueSlug(base);
            entity.setSlug(slug);
            Product saved = productRepository.save(entity);
            log.info("Created product (retry) id={} slug={} weight={}g", saved.getProductId(), saved.getSlug(), saved.getWeightGrams());
            return saved;
        }
    }

    // READ (paged)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<Product> list(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    // READ (detail)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    // UPDATE
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product update(Long id, UpdateProductRequestDTO dto) {
        Product entity = productRepository.findById(id).orElseThrow();

        // kategorie se mění zvlášť (mapper ji ignoruje)
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.getCategoryId()));
            entity.setCategory(cat);
        }

        // ostatní pole přes MapStruct (null neřeší)
        mapper.updateEntity(entity, dto);

        // ⬇ sanity pro váhu jen když přišla (abychom nepřepisovali na 0 omylem)
        if (dto.getWeightGrams() != null) {
            if (entity.getWeightGrams() == null || entity.getWeightGrams() < 0) {
                entity.setWeightGrams(0);
            }
        }

        return productRepository.save(entity);
    }

    // DELETE
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    // ---- helpers ----
    private String ensureUniqueSlug(String base) {
        String s = base; int i = 2;
        while (productRepository.existsBySlug(s)) s = base + "-" + (i++);
        return s;
    }

    private String slugify(String input) {
        if (input == null) input = "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (s.isBlank()) s = "item";
        if (s.length() > 180) s = s.substring(0, 180).replaceAll("(^-|-$)", "");
        return s;
    }
}
