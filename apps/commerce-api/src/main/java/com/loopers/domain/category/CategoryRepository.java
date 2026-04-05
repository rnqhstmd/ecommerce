package com.loopers.domain.category;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAllByIds(Collection<Long> ids);
    List<Category> findAll();
    List<Category> findByParentId(Long parentId);
}
