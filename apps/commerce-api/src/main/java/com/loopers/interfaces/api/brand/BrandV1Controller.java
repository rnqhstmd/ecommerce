package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
