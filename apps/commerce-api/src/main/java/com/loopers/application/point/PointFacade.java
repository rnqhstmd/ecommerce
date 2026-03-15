package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;

    @Transactional(readOnly = true)
    public PointInfo getPoint(String userId) {
        Point point = pointService.getPoint(userId);
        return new PointInfo(userId, point.getBalanceValue());
    }

    @Transactional
    public void chargePoint(PointCommand command) {
        pointService.chargePoint(
                command.userId(),
                command.amount()
        );
    }
}
