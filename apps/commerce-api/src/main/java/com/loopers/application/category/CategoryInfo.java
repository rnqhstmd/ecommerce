package com.loopers.application.category;

import com.loopers.domain.category.Category;

import java.time.ZonedDateTime;

public record CategoryInfo(
        Long categoryId,
        String name,
        Long parentId,
        int depth,
        ZonedDateTime createdAt
) {
    public static CategoryInfo from(Category category) {
        return new CategoryInfo(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getDepth(),
                category.getCreatedAt()
        );
    }
}
