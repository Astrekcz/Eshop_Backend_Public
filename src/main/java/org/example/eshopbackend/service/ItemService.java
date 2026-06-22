package org.example.eshopbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.CreateItemRequestDTO;
import org.example.eshopbackend.dto.UpdateItemRequestDTO;
import org.example.eshopbackend.entity.Category;
import org.example.eshopbackend.entity.Item;
import org.example.eshopbackend.mapper.ItemMapper;
import org.example.eshopbackend.repository.CategoryRepository;
import org.example.eshopbackend.repository.ItemRepository;
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
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final ItemMapper mapper;

    // CREATE
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Item create(CreateItemRequestDTO dto) {
        Category cat = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.categoryId()));

        String base = slugify(dto.name());
        String slug = ensureUniqueSlug(base);

        // Mapování pomocí MapStruct, zbytek musíme dosadit ručně (mapper je ignoruje)
        Item entity = mapper.toEntity(dto);
        entity.setCategory(cat);
        entity.setSlug(slug);

        // Sanity check: null → 0, záporné clampnout
        Integer w = entity.getWeightGrams();
        if (w == null || w < 0) {
            entity.setWeightGrams(0);
        }

        try {
            Item saved = itemRepository.save(entity);
            log.info("Created item id={} slug={} weight={}g", saved.getId(), saved.getSlug(), saved.getWeightGrams());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Fallback pro souběh (unikátní slug)
            slug = ensureUniqueSlug(base);
            entity.setSlug(slug);
            Item saved = itemRepository.save(entity);
            log.info("Created item (retry) id={} slug={} weight={}g", saved.getId(), saved.getSlug(), saved.getWeightGrams());
            return saved;
        }
    }

    // READ (paged)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<Item> list(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    // READ (detail)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Item getById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
    }

    // UPDATE
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Item update(Long id, UpdateItemRequestDTO dto) {
        Item entity = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));

        // Kategorie se mění zvlášť
        if (dto.categoryId() != null) {
            Category cat = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.categoryId()));
            entity.setCategory(cat);
        }

        // Ostatní pole přes MapStruct
        mapper.updateEntity(entity, dto);

        // Sanity check pro váhu
        if (dto.weightGrams() != null) {
            if (entity.getWeightGrams() == null || entity.getWeightGrams() < 0) {
                entity.setWeightGrams(0);
            }
        }

        return itemRepository.save(entity);
    }

    // DELETE
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        itemRepository.deleteById(id);
    }

    // ---- helpers ----
    private String ensureUniqueSlug(String base) {
        String s = base;
        int i = 2;
        while (itemRepository.existsBySlug(s)) {
            s = base + "-" + (i++);
        }
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