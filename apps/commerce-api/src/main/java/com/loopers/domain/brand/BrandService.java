package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;

    public Brand getBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    @Transactional
    public Brand createBrand(String name) {
        Brand brand = Brand.create(name);
        return brandRepository.save(brand);
    }

    public Map<Long, Brand> getBrandsByIds(Collection<Long> ids) {
        return brandRepository.findAllByIds(ids).stream()
                .collect(Collectors.toMap(Brand::getId, Function.identity()));
    }

    @Cacheable(value = "brands", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<Brand> getBrands(Pageable pageable) {
        return brandRepository.findAll(pageable);
    }
}
