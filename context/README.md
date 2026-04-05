# 프로젝트 컨텍스트

도메인 지식, 아키텍처, 용어 사전을 정리하는 공간입니다.

## 프로젝트

| 프로젝트 | 설명 | 상세 |
|----------|------|------|
| ecommerce | Spring Boot 기반 이커머스 플랫폼 (학습/포트폴리오) | [상세](ecommerce/README.md) |

## 도메인

| 도메인 | 설명 | 상세 |
|--------|------|------|
| user | 사용자 계정, 인증/인가 (JWT, RBAC) | [상세](user/README.md) |
| product | 상품 등록/수정/조회/검색/자동완성/집계 | [상세](product/README.md) |
| order | 주문 생성/결제/취소, 보상 트랜잭션 | [상세](order/README.md) |
| cart | Redis Hash 기반 장바구니 | [상세](cart/README.md) |
| brand | 브랜드 관리 | [상세](brand/README.md) |
| category | 계층형 카테고리 | [상세](category/README.md) |
| coupon | 쿠폰 정책/발급, 선착순 동시성 제어 | [상세](coupon/README.md) |
| point | 포인트 잔액/충전/사용/환불 이력 | [상세](point/README.md) |
| like | 상품 좋아요 | [상세](like/README.md) |
| review | 상품 리뷰 (구매 확인 후 작성) | [상세](review/README.md) |
| notification | Kafka consumer 기반 알림 (commerce-streamer) | [상세](notification/README.md) |

## 공통

- [공통 용어 사전](glossary.md)
- [프로젝트 아키텍처](ecommerce/architecture.md)
- [구현 추적](ecommerce/status.md)
