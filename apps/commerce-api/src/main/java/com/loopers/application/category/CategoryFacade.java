package com.loopers.application.category;

import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryFacade {

    private final CategoryService categoryService;

    @Transactional
    public CategoryInfo createCategory(String name, Long parentId) {
        Category category = categoryService.create(name, parentId);
        return CategoryInfo.from(category);
    }

    public List<CategoryInfo> getAllCategories() {
        return categoryService.getAllCategories().stream()
                .map(CategoryInfo::from)
                .toList();
    }
}
