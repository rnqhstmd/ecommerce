package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.interfaces.api.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public PageResponse<BrandInfo> getBrands(Pageable pageable) {
        Page<Brand> brandPage = brandService.getBrands(pageable);
        List<BrandInfo> content = brandPage.getContent().stream()
                .map(BrandInfo::from)
                .toList();
        return PageResponse.of(brandPage, content);
    }
}
