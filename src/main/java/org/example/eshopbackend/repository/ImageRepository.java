// src/main/java/org/example/zeniqbackend/repository/ImageRepository.java
package org.example.eshopbackend.repository;

import org.example.eshopbackend.entity.Image;
import org.example.eshopbackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByProductOrderBySortOrderAscImageIdAsc(Product product);
    int countByProduct(Product product);
    Optional<Image> findFirstByProductAndPrimaryTrue(Product product);
    boolean existsByProductAndPrimaryTrue(Product product);

    @Query("""
        select i.url
        from Image i
        where i.product.productId = :productId
        order by case when i.primary = true then 0 else 1 end,
                 coalesce(i.sortOrder, 0),
                 i.imageId
    """)
    List<String> findTopUrlsByProductId(@Param("productId") Long productId, Pageable pageable);
}
