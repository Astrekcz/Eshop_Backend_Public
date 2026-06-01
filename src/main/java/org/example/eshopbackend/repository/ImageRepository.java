package org.example.eshopbackend.repository;

import org.example.eshopbackend.entity.Image;
import org.example.eshopbackend.entity.Item; // 1. Změna z Product na Item
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    // OPRAVENO: z Product na Item, z ImageIdAsc na IdAsc
    List<Image> findByItemOrderBySortOrderAscIdAsc(Item item);

    // OPRAVENO: z Product na Item
    int countByItem(Item item);

    // OPRAVENO: z Product na Item
    Optional<Image> findFirstByItemAndPrimaryTrue(Item item);

    // OPRAVENO: z Product na Item
    boolean existsByItemAndPrimaryTrue(Item item);

    // OPRAVENO: HQL Query upraveno na i.item.id a i.id
    @Query("""
        select i.url
        from Image i
        where i.item.id = :itemId
        order by case when i.primary = true then 0 else 1 end,
                 coalesce(i.sortOrder, 0),
                 i.id
    """)
    List<String> findTopUrlsByProductId(@Param("itemId") Long itemId, Pageable pageable);
    // ^ Název té metody klidně nech, jak je (pokud ti na něj sahají servisy), 
    // důležité je, že uvnitř anotace @Query jsou ty názvy polí správně podle nových entit.
}