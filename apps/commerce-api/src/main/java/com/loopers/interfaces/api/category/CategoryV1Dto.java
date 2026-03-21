package com.loopers.interfaces.api.category;

import com.loopers.application.category.CategoryInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;

public class CategoryV1Dto {

    public record CreateRequest(
            @NotBlank(message = "카테고리명은 필수입니다.")
            @Size(max = 50, message = "카테고리명은 50자 이하여야 합니다.")
            String name,
            Long parentId
    ) {}

    public record CategoryResponse(
            Long categoryId,
            String name,
            Long parentId,
            int depth,
            ZonedDateTime createdAt
    ) {
        public static CategoryResponse from(CategoryInfo info) {
            return new CategoryResponse(
                    info.categoryId(),
                    info.name(),
                    info.parentId(),
                    info.depth(),
                    info.createdAt()
            );
        }
    }
}
