package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public Point createPoint(String userId) {
        if (pointRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 포인트가 존재하는 사용자입니다.");
        }
        Point point = Point.create(userId);
        return pointRepository.save(point);
    }

    public Point getPoint(String userId) {
        return pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트를 찾을 수 없습니다."));
    }

    private Point getPointWithLock(String userId) {
        return pointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트를 찾을 수 없습니다."));
    }

    @Transactional
    public Point chargePoint(String userId, Long amount) {
        return updatePointAndLog(userId, amount, PointHistoryType.CHARGE, Point::charge);
    }

    @Transactional
    public Point usePoint(String userId, Long amount) {
        return updatePointAndLog(userId, amount, PointHistoryType.USE, Point::use);
    }

    @Transactional
    public Point refundPoint(String userId, Long amount) {
        return updatePointAndLog(userId, amount, PointHistoryType.REFUND, Point::refund);
    }

    private Point updatePointAndLog(String userId, Long amount, PointHistoryType type,
                                     java.util.function.BiConsumer<Point, Long> operation) {
        Point point = getPointWithLock(userId);
        operation.accept(point, amount);
        pointRepository.save(point);
        pointHistoryRepository.save(
                PointHistory.create(point, type, amount, point.getBalanceValue())
        );
        return point;
    }

    public Page<PointHistory> getPointHistory(Long pointId, Pageable pageable) {
        return pointHistoryRepository.findByPointId(pointId, pageable);
    }
}
