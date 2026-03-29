package com.loopers.domain.product;

/**
 * 상품 삭제 도메인 이벤트.
 *
 * 참고: ProductDeletedEvent 발행은 상품 삭제 API 구현 시 추가.
 * 현 Phase에서는 이벤트 정의와 수신(ProductIndexer.handleProductDeleted)만 구현한다.
 */
public record ProductDeletedEvent(Long productId) {}
