package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.ZonedDateTime;

@Entity
@Getter
@Table(name = "coupon_policies")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Long discountValue;

    @Column(name = "valid_from", nullable = false)
    private ZonedDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private ZonedDateTime validTo;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private int issuedQuantity;

    private CouponPolicy(String name, DiscountType discountType, Long discountValue,
                          ZonedDateTime validFrom, ZonedDateTime validTo, int totalQuantity) {
        validateFields(name, discountType, discountValue, validFrom, validTo, totalQuantity);
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
    }

    public static CouponPolicy create(String name, DiscountType discountType, Long discountValue,
                                        ZonedDateTime validFrom, ZonedDateTime validTo, int totalQuantity) {
        return new CouponPolicy(name, discountType, discountValue, validFrom, validTo, totalQuantity);
    }

    public void increaseIssuedQuantity() {
        this.issuedQuantity++;
    }

    public boolean isValid() {
        ZonedDateTime now = ZonedDateTime.now();
        return !now.isBefore(validFrom) && !now.isAfter(validTo);
    }

    public long calculateDiscount(long totalAmount) {
        if (discountType == DiscountType.RATE) {
            long discount = totalAmount * discountValue / 100;
            return Math.min(discount, totalAmount);
        } else {
            return Math.min(discountValue, totalAmount);
        }
    }

    private void validateFields(String name, DiscountType discountType, Long discountValue,
                                 ZonedDateTime validFrom, ZonedDateTime validTo, int totalQuantity) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 필수입니다.");
        }
        if (discountType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 타입은 필수입니다.");
        }
        if (discountValue == null || discountValue <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인값은 1 이상이어야 합니다.");
        }
        if (validFrom == null || validTo == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효기간은 필수입니다.");
        }
        if (validFrom.isAfter(validTo)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효기간 시작일은 종료일보다 이전이어야 합니다.");
        }
        if (totalQuantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 수량은 1 이상이어야 합니다.");
        }
    }
}
