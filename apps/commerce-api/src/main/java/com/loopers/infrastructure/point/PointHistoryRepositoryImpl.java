package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointHistory;
import com.loopers.domain.point.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryJpaRepository jpaRepository;

    @Override
    public PointHistory save(PointHistory history) {
        return jpaRepository.save(history);
    }

    @Override
    public Page<PointHistory> findByPointId(Long pointId, Pageable pageable) {
        return jpaRepository.findByPointIdOrderByCreatedAtDesc(pointId, pageable);
    }
}
