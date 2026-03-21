package com.loopers.interfaces.api.category;

import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryService;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryV1Controller {

    private final CategoryService categoryService;

    @PostMapping
    public ApiResponse<CategoryV1Dto.CategoryResponse> createCategory(
            @RequestBody @Valid CategoryV1Dto.CreateRequest request
    ) {
        Category category = categoryService.create(request.name(), request.parentId());
        return ApiResponse.success(CategoryV1Dto.CategoryResponse.from(category));
    }

    @GetMapping
    public ApiResponse<List<CategoryV1Dto.CategoryResponse>> getCategories() {
        List<CategoryV1Dto.CategoryResponse> categories = categoryService.getAllCategories().stream()
                .map(CategoryV1Dto.CategoryResponse::from)
                .toList();
        return ApiResponse.success(categories);
    }
}
