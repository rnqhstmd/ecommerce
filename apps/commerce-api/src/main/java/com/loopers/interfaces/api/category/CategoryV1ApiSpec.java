package com.loopers.interfaces.api.category;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Category API", description = "카테고리 관리 API")
public interface CategoryV1ApiSpec {

    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 등록합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "카테고리 생성 성공",
                    content = @Content(schema = @Schema(implementation = CategoryV1Dto.CategoryResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<CategoryV1Dto.CategoryResponse> createCategory(
            @Parameter(description = "카테고리 생성 요청 정보", required = true)
            @RequestBody CategoryV1Dto.CreateRequest request
    );

    @Operation(summary = "카테고리 목록 조회", description = "전체 카테고리 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CategoryV1Dto.CategoryResponse.class))
            )
    })
    ApiResponse<List<CategoryV1Dto.CategoryResponse>> getCategories();
}
