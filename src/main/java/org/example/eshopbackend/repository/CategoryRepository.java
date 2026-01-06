package org.example.eshopbackend.repository;

import org.example.eshopbackend.entity.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // findById(...) už dědíš z JpaRepository
    boolean existsBySlug(String slug);
    default List<Category> findAllOrdered() {
        return findAll(Sort.by(Sort.Direction.ASC, "categoryName"));

    }
}
