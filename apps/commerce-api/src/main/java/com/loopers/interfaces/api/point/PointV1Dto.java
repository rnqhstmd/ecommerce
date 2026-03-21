package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointCommand;
import com.loopers.domain.point.PointHistory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.ZonedDateTime;

public class PointV1Dto {
    public record ChargeRequest(
            @NotNull(message = "충전 금액은 필수입니다.")
            @Positive(message = "충전 금액은 0보다 커야 합니다.")
            Long amount
    ) {
        public PointCommand toCommand(String userId) {
            return new PointCommand(
                    userId,
                    this.amount
            );
        }
    }

    public record PointResponse(
            String userId,
            Long amount
    ) {
        public static PointResponse of(
                String userId,
                Long amount
        ) {
            return new PointResponse(
                    userId,
                    amount
            );
        }
    }

    public record PointHistoryResponse(
            Long historyId,
            String type,
            Long amount,
            Long balanceAfter,
            ZonedDateTime createdAt
    ) {
        public static PointHistoryResponse from(PointHistory history) {
            return new PointHistoryResponse(
                    history.getId(),
                    history.getType().name(),
                    history.getAmount(),
                    history.getBalanceAfter(),
                    history.getCreatedAt()
            );
        }
    }
}
