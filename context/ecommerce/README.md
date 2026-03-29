# ecommerce

Spring Boot 3.4.4 / Java 21 기반 멀티모듈 이커머스 플랫폼. 학습 및 포트폴리오 목적으로 구축.

## 배경

이커머스 도메인(상품, 주문, 결제, 포인트, 좋아요)을 실습하며 클린 아키텍처, 멀티모듈, 인프라(MySQL, Redis master-replica, Kafka KRaft) 구성을 학습하는 개인 프로젝트이다. 주차별 요구사항(docs/week2 등)을 구현하며 자유롭게 진행 중이다.

## 안 하면 어떻게 되는가

개인적인 학습/커리어 목표에만 영향. 외부 데드라인이나 사용자 영향 없음.

## 사용자와 규모

- 개발자 1명 (본인)
- 로컬 환경에서 Docker Compose로 인프라 실행
- 실사용자 대상 서비스 아님

## 성공 기준

- 주차별 요구사항 구현 완료
- 클린 아키텍처 패턴 적용 (도메인 → 애플리케이션 → 인프라 → 인터페이스 레이어)
- 인프라 구성 경험 축적 (Redis replica, Kafka, Prometheus/Grafana)
- 포트폴리오로 활용 가능한 수준의 코드 품질

## 현재 상태

Phase 1~6 전체 구현 완료. Week 2 요구사항 + 확장 로드맵 6단계 모두 머지됨. 컨트롤러 11개, 도메인 10개 (User, Order, Product, Brand, Point, Like, Cart, Review, Coupon, Category). 테스트 전체 통과.

## 확장 로드맵

| Phase | 주제 | 핵심 학습 포인트 | 상태 |
|-------|------|-----------------|------|
| 1 | 미노출 API 연결 + Week 2 완성 | 페이징 응답, 조건부 필드 (isLiked) | ✅ 6/6 (PR #5) |
| 2 | 주문 생명주기 + 포인트 이력 | 보상 트랜잭션, QueryDSL 동적 쿼리, Audit Trail | ✅ 5/5 (PR #7) |
| 3 | 장바구니 + 상품 관리 확장 | Redis Hash/Sorted Set, 캐시 무효화 전략 | ✅ 9/9 (PR #8) |
| 4 | 리뷰 + 쿠폰 + 알림 + 카테고리 | 선착순 동시성(Redis DECR), Kafka consumer, 계층형 모델 | ✅ 9/9 (PR #9) |
| 5 | 운영 안정성 + 성능 최적화 | 분산 락, Rate Limiting, Circuit Breaker, Cursor Pagination | ✅ 6/6 (PR #10) |
| 6 | Spring Security + JWT 인증/인가 | JWT Access/Refresh, RBAC, Spring Security 필터 체인 | ✅ 6/6 (PR #11) |
| 7 | 검색 + 개인화 (Elasticsearch) | Nori 한글 형태소 분석, 역인덱스, 자동완성, Faceted Search | ⬜ 11건 |

상세: [status.md](status.md) / 도입 배경: [elasticsearch-adoption.md](elasticsearch-adoption.md)
