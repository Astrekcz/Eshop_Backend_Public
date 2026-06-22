package org.example.eshopbackend.repository;

import org.example.eshopbackend.entity.Item;
import org.example.eshopbackend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findBySlug(String slug);

    boolean existsBySlug(String slug);
    // Pokud máš v Product pole "category" typu Category:
    @Query("select p from Item p join p.category c where c.slug = :slug")
    Page<Item> findByCategorySlug(@Param("slug") String slug, Pageable pageable);

    //fulltext-like vyhledávání (case-insensitive)
    @Query("""
        select p from Item p
        left join p.category c
        where
            lower(p.name) like lower(concat('%', :q, '%'))
         or lower(coalesce(p.description, '')) like lower(concat('%', :q, '%'))
         or lower(p.slug) like lower(concat('%', :q, '%'))
         or lower(coalesce(c.categoryName, '')) like lower(concat('%', :q, '%'))
    """)
    Page<Item> search(@Param("q") String q, Pageable pageable);

}
