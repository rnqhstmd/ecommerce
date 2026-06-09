package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandFacadeTest {

    @Mock
    private BrandService brandService;

    @InjectMocks
    private BrandFacade brandFacade;

    @DisplayName("브랜드를 생성하면 BrandService.createBrand를 호출하고 BrandInfo를 반환한다.")
    @Test
    void createBrand_delegatesToBrandService() {
        // given
        Brand brand = Brand.create("Nike");
        when(brandService.createBrand("Nike")).thenReturn(brand);

        // when
        BrandInfo result = brandFacade.createBrand("Nike");

        // then
        verify(brandService, times(1)).createBrand("Nike");
        assertThat(result.name()).isEqualTo("Nike");
    }

    @DisplayName("브랜드를 조회하면 BrandService.getBrand를 호출하고 BrandInfo를 반환한다.")
    @Test
    void getBrand_delegatesToBrandService() {
        // given
        Brand brand = Brand.create("Adidas");
        when(brandService.getBrand(1L)).thenReturn(brand);

        // when
        BrandInfo result = brandFacade.getBrand(1L);

        // then
        verify(brandService, times(1)).getBrand(1L);
        assertThat(result.name()).isEqualTo("Adidas");
    }
}
