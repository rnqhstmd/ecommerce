package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "orders")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Embedded
    private OrderTotalAmount totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "paid_at")
    private ZonedDateTime paidAt;

    @Column(name = "cancelled_at")
    private ZonedDateTime cancelledAt;

    @org.hibernate.annotations.BatchSize(size = 100)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(String userId) {
        validateUserId(userId);
        this.userId = userId;
        this.totalAmount = OrderTotalAmount.zero();
        this.status = OrderStatus.PENDING;
    }

    public static Order create(String userId) {
        return new Order(userId);
    }

    public void addOrderItem(Long productId, String productName, Long unitPrice, Integer quantity) {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.");
        }

        OrderItem orderItem = OrderItem.create(productId, productName, unitPrice, quantity);
        this.orderItems.add(orderItem);
        orderItem.assignOrder(this);

        recalculateTotalAmount();
    }

    public void completePayment() {
        validateBeforePayment();
        this.status = OrderStatus.PAID;
        this.paidAt = ZonedDateTime.now();
    }

    public void cancel() {
        if (this.status != OrderStatus.PAID) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "결제 완료 상태의 주문만 취소할 수 있습니다.");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = ZonedDateTime.now();
    }

    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }

    private void recalculateTotalAmount() {
        this.totalAmount = this.orderItems.stream()
                .map(OrderItem::calculateAmount)
                .reduce(OrderTotalAmount.zero(), OrderTotalAmount::add);
    }

    private void validateBeforePayment() {
        if (this.orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.");
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    public Long getTotalAmountValue() {
        return this.totalAmount.getValue();
    }

    public List<OrderItem> getOrderItems() {
        return List.copyOf(orderItems);
    }
}
