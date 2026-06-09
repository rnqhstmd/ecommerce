package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointFacadeTest {

    @Mock
    private PointService pointService;

    @InjectMocks
    private PointFacade pointFacade;

    @DisplayName("포인트를 조회하면 PointService.getPoint를 호출하고 잔액을 담은 PointInfo를 반환한다.")
    @Test
    void getPoint_delegatesToPointService() {
        // given
        Point point = Point.create("testuser", 1500L);
        when(pointService.getPoint("testuser")).thenReturn(point);

        // when
        PointInfo result = pointFacade.getPoint("testuser");

        // then
        verify(pointService, times(1)).getPoint("testuser");
        assertThat(result.userId()).isEqualTo("testuser");
        assertThat(result.balance()).isEqualTo(1500L);
    }

    @DisplayName("포인트를 충전하면 PointService.chargePoint를 userId/amount로 호출하고 충전 후 잔액을 반환한다.")
    @Test
    void chargePoint_delegatesToPointService() {
        // given
        PointCommand command = new PointCommand("testuser", 1000L);
        Point point = Point.create("testuser", 3000L);
        when(pointService.chargePoint("testuser", 1000L)).thenReturn(point);

        // when
        PointInfo result = pointFacade.chargePoint(command);

        // then
        verify(pointService, times(1)).chargePoint("testuser", 1000L);
        assertThat(result.userId()).isEqualTo("testuser");
        assertThat(result.balance()).isEqualTo(3000L);
    }
}
