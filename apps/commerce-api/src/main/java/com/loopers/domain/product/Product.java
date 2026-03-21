package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "products")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Embedded
    private ProductPrice price;

    @Embedded
    private Stock stock;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    private Product(String name, ProductPrice price, Integer stock, Long brandId) {
        validateRequiredFields(name, price, stock, brandId);
        this.name = name;
        this.price = price;
        this.stock = Stock.of(stock);
        this.brandId = brandId;
    }

    public static Product create(String name, Long price, Integer stock, Long brandId) {
        return new Product(name, ProductPrice.of(price), stock, brandId);
    }

    private void validateRequiredFields(String name, ProductPrice price, Integer stock, Long brandId) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 필수입니다.");
        }
        if (stock == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 필수입니다.");
        }
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드는 필수입니다.");
        }
    }

    public void decreaseStock(Integer quantity) {
        this.stock = this.stock.decrease(quantity);
    }

    public boolean isStockAvailable(Integer quantity) {
        return this.stock.isAvailable(quantity);
    }

    public Integer getStockValue() {
        return this.stock.getValue();
    }

    public Long getPriceValue() {
        return this.price.getValue();
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
