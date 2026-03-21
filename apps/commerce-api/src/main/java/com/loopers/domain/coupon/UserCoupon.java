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
@Table(name = "user_coupons", uniqueConstraints = @UniqueConstraint(
        name = "uq_user_coupons_user_policy", columnNames = {"user_id", "coupon_policy_id"}))
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "coupon_policy_id", nullable = false)
    private Long couponPolicyId;

    @Column(name = "issued_at", nullable = false)
    private ZonedDateTime issuedAt;

    @Column(name = "used_at")
    private ZonedDateTime usedAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    private UserCoupon(String userId, Long couponPolicyId) {
        this.userId = userId;
        this.couponPolicyId = couponPolicyId;
        this.issuedAt = ZonedDateTime.now();
        this.used = false;
    }

    public static UserCoupon create(String userId, Long couponPolicyId) {
        return new UserCoupon(userId, couponPolicyId);
    }

    public void markUsed() {
        if (this.used) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        this.used = true;
        this.usedAt = ZonedDateTime.now();
    }

    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }
}
