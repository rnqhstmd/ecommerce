package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointCommand;
import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

        pointFacade.chargePoint(command);
        PointInfo pointInfo = pointFacade.getPoint(userId);

        return ApiResponse.success(PointV1Dto.PointResponse.of(pointInfo.userId(), pointInfo.balance()));
    }
}
