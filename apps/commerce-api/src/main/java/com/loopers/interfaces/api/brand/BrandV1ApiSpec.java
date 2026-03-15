package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Brand API", description = "브랜드 관리 API")
public interface BrandV1ApiSpec {

    @Operation(summary = "브랜드 생성", description = "새로운 브랜드를 등록합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 생성 성공",
                    content = @Content(schema = @Schema(implementation = BrandV1Dto.BrandResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<BrandV1Dto.BrandResponse> createBrand(
            @Parameter(description = "브랜드 생성 요청 정보", required = true)
            @RequestBody BrandV1Dto.CreateRequest request
    );
}
