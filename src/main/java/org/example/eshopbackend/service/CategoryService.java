package org.example.eshopbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.CreateCategoryRequestDTO;
import org.example.eshopbackend.entity.Category;
import org.example.eshopbackend.repository.CategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Category addCategory(CreateCategoryRequestDTO dto) {
        // 1) připrav slug (použij z DTO, nebo jej vygeneruj z názvu)
        String base = (dto.getSlug() == null || dto.getSlug().isBlank())
                ? slugify(dto.getCategoryName())
                : normalizeSlug(dto.getSlug());

        // omez délku kvůli @Column(length=100)
        if (base.length() > 100) base = base.substring(0, 100).replaceAll("(^-|-$)", "");

        // 2) zajisti unikátnost
        String slug = ensureUniqueSlug(base);

        // 3) ulož
        Category entity = Category.builder()
                .categoryName(dto.getCategoryName())
                .slug(slug)
                .build();

        try {
            Category saved = categoryRepository.save(entity);
            log.info("Created category id={} slug={}", saved.getCategoryId(), saved.getSlug());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // souběh – přidáme další sufix a zkusíme znovu
            slug = ensureUniqueSlug(base);
            entity.setSlug(slug);
            Category saved = categoryRepository.save(entity);
            log.info("Created category (retry) id={} slug={}", saved.getCategoryId(), saved.getSlug());
            return saved;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAll(Sort.by(Sort.Order.asc("categoryName")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id).orElseThrow();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    // --- helpers ---

    private String ensureUniqueSlug(String base) {
        String s = base; int i = 2;
        while (categoryRepository.existsBySlug(s)) {
            s = base;
            // omezíme, aby s přidaným sufixem nepřesáhl 100 znaků
            String suffix = "-" + (i++);
            int maxBaseLen = 100 - suffix.length();
            if (s.length() > maxBaseLen) s = s.substring(0, maxBaseLen).replaceAll("(^-|-$)", "");
            s = s + suffix;
        }
        return s;
    }

    private String normalizeSlug(String input) {
        String s = input == null ? "" : input.toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (s.isBlank()) s = "kategorie";
        return s;
    }

    private String slugify(String input) {
        String s = input == null ? "" : input;
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (s.isBlank()) s = "kategorie";
        return s;
    }
}
