package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeCommand;
import com.loopers.application.like.LikeFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping
    @Override
    public ApiResponse<LikeV1Dto.LikeResponse> addLike(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestBody @Valid LikeV1Dto.LikeRequest request
    ) {
        validateUserId(userId);
        likeFacade.addLike(new LikeCommand(userId, request.productId()));
        return ApiResponse.success(LikeV1Dto.LikeResponse.of(userId, request.productId()));
    }

    @DeleteMapping("/{productId}")
    @Override
    public ApiResponse<Void> removeLike(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PathVariable Long productId
    ) {
        validateUserId(userId);
        likeFacade.removeLike(new LikeCommand(userId, productId));
        return ApiResponse.success(null);
    }

    @GetMapping
    @Override
    public ApiResponse<List<LikeV1Dto.LikeItemResponse>> getMyLikes(
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        validateUserId(userId);
        List<Long> productIds = likeFacade.getMyLikes(userId);
        List<LikeV1Dto.LikeItemResponse> responses = productIds.stream()
                .map(LikeV1Dto.LikeItemResponse::of).toList();
        return ApiResponse.success(responses);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }
    }
}
