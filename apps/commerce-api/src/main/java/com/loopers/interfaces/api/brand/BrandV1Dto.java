package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import jakarta.validation.constraints.NotBlank;

public class BrandV1Dto {

    public record CreateRequest(
            @NotBlank(message = "브랜드명은 필수입니다.")
            String name
    ) {}

    public record BrandResponse(
            Long id,
            String name
    ) {
        public static BrandResponse from(BrandInfo info) {
            return new BrandResponse(info.id(), info.name());
        }
    }
}
