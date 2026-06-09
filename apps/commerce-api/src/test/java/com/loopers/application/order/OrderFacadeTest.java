package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderPlacedEvent;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.StockDeductionService;
import com.loopers.domain.product.StockDeductionService.StockDeductionCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private OrderService orderService;

    @Mock
    private StockDeductionService stockDeductionService;

    @Mock
    private PointService pointService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderFacade orderFacade;

    @DisplayName("주문 시 재고 차감 → 포인트 차감 → 주문 저장 → 이벤트 발행이 순서대로 호출된다.")
    @Test
    void placeOrder_invokesStockDeductionPointUseSaveAndEvent() {
        // given
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(100L);
        when(product.getName()).thenReturn("Test Product");
        when(product.getPriceValue()).thenReturn(5000L);

        when(stockDeductionService.deductStock(anyList())).thenReturn(List.of(product));
        when(orderService.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPlaceCommand command = new OrderPlaceCommand(
                "testuser",
                List.of(new OrderPlaceCommand.OrderItemCommand(100L, 2))
        );

        // when
        OrderInfo result = orderFacade.placeOrder(command);

        // then — 재고 차감 호출 검증
        ArgumentCaptor<List<StockDeductionCommand>> deductionCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockDeductionService, times(1)).deductStock(deductionCaptor.capture());
        assertThat(deductionCaptor.getValue()).hasSize(1);
        assertThat(deductionCaptor.getValue().get(0).productId()).isEqualTo(100L);
        assertThat(deductionCaptor.getValue().get(0).quantity()).isEqualTo(2);

        // then — 포인트 차감 호출 검증 (단가 5000 * 수량 2 = 10000)
        verify(pointService, times(1)).usePoint("testuser", 10000L);

        // then — 주문 저장 호출 검증
        verify(orderService, times(1)).save(any(Order.class));

        // then — 이벤트 발행 검증
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        OrderPlacedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.userId()).isEqualTo("testuser");
        assertThat(publishedEvent.totalAmount()).isEqualTo(10000L);
        assertThat(publishedEvent.items()).hasSize(1);
        assertThat(publishedEvent.items().get(0).productId()).isEqualTo(100L);

        // then — 반환 결과 검증
        assertThat(result.userId()).isEqualTo("testuser");
        assertThat(result.totalAmount()).isEqualTo(10000L);
        assertThat(result.items()).hasSize(1);
    }

    @DisplayName("내 주문 목록을 조회하면 OrderService.getOrdersByUserId를 호출하고 OrderInfo 목록을 반환한다.")
    @Test
    void getMyOrders_delegatesToOrderService() {
        // given
        Order order = Order.create("testuser");
        when(orderService.getOrdersByUserId("testuser")).thenReturn(List.of(order));

        // when
        List<OrderInfo> result = orderFacade.getMyOrders("testuser");

        // then
        verify(orderService, times(1)).getOrdersByUserId("testuser");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo("testuser");
    }

    @DisplayName("주문 상세를 조회하면 OrderService.getOrderByIdAndUserId를 호출하고 OrderInfo를 반환한다.")
    @Test
    void getOrderDetail_delegatesToOrderService() {
        // given
        Order order = Order.create("testuser");
        when(orderService.getOrderByIdAndUserId(1L, "testuser")).thenReturn(order);

        // when
        OrderInfo result = orderFacade.getOrderDetail(1L, "testuser");

        // then
        verify(orderService, times(1)).getOrderByIdAndUserId(1L, "testuser");
        assertThat(result.userId()).isEqualTo("testuser");
    }
}
