package com.loopers.domain.point;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointHistoryRepository {
    PointHistory save(PointHistory history);
    Page<PointHistory> findByPointId(Long pointId, Pageable pageable);
}
