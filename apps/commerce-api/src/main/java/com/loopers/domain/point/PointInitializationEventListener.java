package com.loopers.domain.point;

import com.loopers.domain.user.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PointInitializationEventListener {

    private final PointService pointService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(UserSignedUpEvent event) {
        pointService.createPoint(event.userId());
    }
}
