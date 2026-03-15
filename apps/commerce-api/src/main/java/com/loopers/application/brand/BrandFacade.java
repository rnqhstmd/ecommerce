package com.loopers.application.brand;

import com.loopers.domain.brand.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandFacade {

    private final BrandService brandService;

    @Transactional
    public BrandInfo createBrand(String name) {
        return BrandInfo.from(brandService.createBrand(name));
    }

    public BrandInfo getBrand(Long id) {
        return BrandInfo.from(brandService.getBrand(id));
    }
}
