package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "point_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_id", nullable = false)
    private Point point;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private PointHistoryType type;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    private PointHistory(Point point, PointHistoryType type, Long amount, Long balanceAfter) {
        this.point = point;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public static PointHistory create(Point point, PointHistoryType type, Long amount, Long balanceAfter) {
        return new PointHistory(point, type, amount, balanceAfter);
    }
}
