package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.loopers.domain.product.QProduct.product;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Product> findProducts(ProductSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.brandId() != null) {
            builder.and(product.brandId.eq(condition.brandId()));
        }
        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            builder.and(product.name.containsIgnoreCase(condition.keyword().trim()));
        }
        if (condition.minPrice() != null) {
            builder.and(product.price.value.goe(condition.minPrice()));
        }
        if (condition.maxPrice() != null) {
            builder.and(product.price.value.loe(condition.maxPrice()));
        }

        builder.and(product.deletedAt.isNull());

        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .where(builder)
                .offset(condition.pageable().getOffset())
                .limit(condition.pageable().getPageSize());

        for (OrderSpecifier<?> orderSpecifier : getOrderSpecifiers(condition.pageable().getSort())) {
            query.orderBy(orderSpecifier);
        }

        List<Product> content = query.fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, condition.pageable(), total != null ? total : 0L);
    }

    private List<OrderSpecifier<?>> getOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sort.isUnsorted()) {
            orderSpecifiers.add(product.id.desc());
            return orderSpecifiers;
        }

        for (Sort.Order order : sort) {
            ComparableExpressionBase<?> path = switch (order.getProperty()) {
                case "createdAt" -> product.createdAt;
                case "price.value" -> product.price.value;
                case "likeCount" -> product.likeCount;
                case "id" -> product.id;
                default -> product.id;
            };
            orderSpecifiers.add(order.isAscending() ? path.asc() : path.desc());
        }

        return orderSpecifiers;
    }
}
