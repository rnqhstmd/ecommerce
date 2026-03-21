package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "order_items")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Embedded
    private OrderItemPrice orderItemPrice;

    private OrderItem(Long productId, String productName, Long unitPrice, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.orderItemPrice = OrderItemPrice.of(unitPrice);
    }

    public static OrderItem create(Long productId, String productName, Long unitPrice, Integer quantity) {
        return new OrderItem(productId, productName, unitPrice, quantity);
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public OrderTotalAmount calculateAmount() {
        return OrderTotalAmount.of(this.orderItemPrice.getValue() * this.quantity);
    }

    public Long getUnitPriceValue() {
        return this.orderItemPrice.getValue();
    }
}
