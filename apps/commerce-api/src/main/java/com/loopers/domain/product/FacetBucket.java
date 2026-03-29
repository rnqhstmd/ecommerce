package com.loopers.domain.product;

/** ProductSearchPort의 반환 타입. 검색 인프라 구현체에서 생성하여 Application 레이어로 전달한다. */
public record FacetBucket(String name, long count) {}
