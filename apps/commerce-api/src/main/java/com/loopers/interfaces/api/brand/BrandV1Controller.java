package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandV1Controller implements BrandV1ApiSpec {

    private final BrandFacade brandFacade;

    @PostMapping
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> createBrand(
            @RequestBody @Valid BrandV1Dto.CreateRequest request
    ) {
        BrandInfo brandInfo = brandFacade.createBrand(request.name());
        return ApiResponse.success(BrandV1Dto.BrandResponse.from(brandInfo));
    }

    @GetMapping("/{id}")
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> getBrand(@PathVariable Long id) {
        BrandInfo brandInfo = brandFacade.getBrand(id);
        return ApiResponse.success(BrandV1Dto.BrandResponse.from(brandInfo));
    }

    @GetMapping
    @Override
    public ApiResponse<BrandV1Dto.BrandListResponse> getBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "page는 0 이상, size는 1~100 이어야 합니다.");
        }
        PageResponse<BrandInfo> pageResponse = brandFacade.getBrands(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        );
        return ApiResponse.success(BrandV1Dto.BrandListResponse.from(pageResponse));
    }
}
