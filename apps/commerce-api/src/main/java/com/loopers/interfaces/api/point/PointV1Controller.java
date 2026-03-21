package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointCommand;
import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.domain.point.PointHistory;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @GetMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getPoint(
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }

        PointInfo pointInfo = pointFacade.getPoint(userId);
        return ApiResponse.success(PointV1Dto.PointResponse.of(pointInfo.userId(), pointInfo.balance()));
    }

    @PostMapping("/charge")
    @Override
    public ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestBody @Valid PointV1Dto.ChargeRequest request
    ) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }

        PointCommand command = request.toCommand(userId);
        PointInfo pointInfo = pointFacade.chargePoint(command);

        return ApiResponse.success(PointV1Dto.PointResponse.of(pointInfo.userId(), pointInfo.balance()));
    }

    @GetMapping("/history")
    @Override
    public ApiResponse<PageResponse<PointV1Dto.PointHistoryResponse>> getPointHistory(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "X-USER-ID 헤더는 필수입니다.");
        }
        if (size < 1 || size > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "size는 1 이상 100 이하여야 합니다.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<PointHistory> historyPage = pointFacade.getPointHistory(userId, pageable);

        List<PointV1Dto.PointHistoryResponse> content = historyPage.getContent().stream()
                .map(PointV1Dto.PointHistoryResponse::from)
                .toList();

        return ApiResponse.success(PageResponse.of(historyPage, content));
    }
}
