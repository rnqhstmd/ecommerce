package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import com.loopers.interfaces.api.common.PageResponse;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

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

    public record BrandListResponse(
            List<BrandResponse> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static BrandListResponse from(PageResponse<BrandInfo> pageResponse) {
            List<BrandResponse> contents = pageResponse.content().stream()
                    .map(BrandResponse::from)
                    .toList();
            return new BrandListResponse(
                    contents,
                    pageResponse.currentPage(),
                    pageResponse.size(),
                    pageResponse.totalElements(),
                    pageResponse.totalPages()
            );
        }
    }
}
