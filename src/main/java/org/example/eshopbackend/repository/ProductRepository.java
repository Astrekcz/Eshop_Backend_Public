package org.example.eshopbackend.repository;

import org.example.eshopbackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;          // ✅ SPRÁVNÝ Page
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByProductId(Long productID);
    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);
    // Pokud máš v Product pole "category" typu Category:
    @Query("select p from Product p join p.category c where c.slug = :slug")
    Page<Product> findByCategorySlug(@Param("slug") String slug, Pageable pageable);

    // 🔎 fulltext-like vyhledávání (case-insensitive)
    @Query("""
        select p from Product p
        left join p.category c
        where
            lower(p.productName) like lower(concat('%', :q, '%'))
         or lower(coalesce(p.description, '')) like lower(concat('%', :q, '%'))
         or lower(p.slug) like lower(concat('%', :q, '%'))
         or lower(coalesce(c.categoryName, '')) like lower(concat('%', :q, '%'))
    """)
    Page<Product> search(@Param("q") String q, Pageable pageable);

}
