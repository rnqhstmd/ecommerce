package com.loopers.infrastructure.category;

import com.loopers.domain.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentId(Long parentId);
}
