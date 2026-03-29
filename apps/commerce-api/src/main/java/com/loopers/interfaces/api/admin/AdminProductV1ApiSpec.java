package com.loopers.interfaces.api.admin;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Product API", description = "상품 관리 ADMIN API")
public interface AdminProductV1ApiSpec {

    @Operation(summary = "상품 재인덱싱", description = "MySQL 전체 상품을 Elasticsearch에 재인덱싱합니다. (ADMIN 전용)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재인덱싱 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    ApiResponse<AdminProductV1Dto.ReindexResponse> reindex();
}
