package com.loopers.interfaces.api.category;

import com.loopers.application.category.CategoryFacade;
import com.loopers.application.category.CategoryInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryV1Controller implements CategoryV1ApiSpec {

    private final CategoryFacade categoryFacade;

    @PostMapping
    @Override
    public ApiResponse<CategoryV1Dto.CategoryResponse> createCategory(
            @RequestBody @Valid CategoryV1Dto.CreateRequest request
    ) {
        CategoryInfo info = categoryFacade.createCategory(request.name(), request.parentId());
        return ApiResponse.success(CategoryV1Dto.CategoryResponse.from(info));
    }

    @GetMapping
    @Override
    public ApiResponse<List<CategoryV1Dto.CategoryResponse>> getCategories() {
        List<CategoryV1Dto.CategoryResponse> categories = categoryFacade.getAllCategories().stream()
                .map(CategoryV1Dto.CategoryResponse::from)
                .toList();
        return ApiResponse.success(categories);
    }
}
