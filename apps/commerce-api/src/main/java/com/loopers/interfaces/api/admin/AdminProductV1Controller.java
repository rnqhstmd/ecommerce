package com.loopers.interfaces.api.admin;

import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductV1Controller implements AdminProductV1ApiSpec {

    private final ProductFacade productFacade;

    @PostMapping("/reindex")
    @Override
    public ApiResponse<AdminProductV1Dto.ReindexResponse> reindex() {
        long indexed = productFacade.reindexAll();
        return ApiResponse.success(new AdminProductV1Dto.ReindexResponse(indexed));
    }
}
