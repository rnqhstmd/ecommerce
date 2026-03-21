# Ecommerce Platform

> Spring Boot 3.4 / Java 21 기반 멀티모듈 이커머스 백엔드

클린 아키텍처와 DDD를 적용한 이커머스 플랫폼입니다.
상품 조회, 주문/결제, 포인트, 좋아요 등 핵심 도메인을 구현하고 있으며,
MySQL, Redis Master-Replica, Kafka(KRaft) 기반의 실서비스급 인프라를 Docker Compose로 구성합니다.

---

## Tech Stack

| 영역 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.4, Spring Cloud |
| Build | Gradle Kotlin DSL (멀티모듈) |
| Database | MySQL 8.0, Spring Data JPA, QueryDSL |
| Cache | Redis 7.0 (Master-Replica 구조) |
| Messaging | Apache Kafka 3.5 (KRaft mode, ZooKeeper-less) |
| Monitoring | Prometheus + Grafana |
| Test | JUnit 5, Testcontainers, MockK, Instancio |
| Infra | Docker Compose |

---

## Architecture

### Multi-Module 구조

```
ecommerce/
├── apps/                          # 실행 가능한 Spring Boot 애플리케이션
│   ├── commerce-api               # REST API 서버 (:8080)
│   └── commerce-streamer          # Kafka Consumer 서버
│
├── modules/                       # 재사용 가능한 인프라 설정
│   ├── jpa                        # JPA + QueryDSL + MySQL 설정
│   ├── redis                      # Redis Master/Replica 설정
│   └── kafka                      # Kafka Producer/Consumer 설정
│
└── supports/                      # 부가 기능 모듈
    ├── jackson                    # JSON 직렬화 설정
    ├── logging                    # 로깅 설정
    └── monitoring                 # Prometheus 메트릭 설정
```

### Clean Architecture (Layer 구조)

```
┌──────────────────────────────────────────────────────────┐
│  Interface Layer     Controller, DTO, ApiSpec            │
├──────────────────────────────────────────────────────────┤
│  Application Layer   Facade, Command, Info               │
├──────────────────────────────────────────────────────────┤
│  Domain Layer        Entity, Value Object, Service,      │
│                      Repository (interface), Event        │
├──────────────────────────────────────────────────────────┤
│  Infrastructure      RepositoryImpl, JpaRepository,      │
│                      Config, EventPublisher               │
└──────────────────────────────────────────────────────────┘
```

- **Interface**: HTTP 요청/응답을 처리. Controller는 ApiSpec 인터페이스를 구현하여 문서화와 구현을 분리
- **Application**: Facade 패턴으로 트랜잭션 경계를 관리하고 도메인 서비스를 오케스트레이션
- **Domain**: 핵심 비즈니스 로직. 외부 의존성 없이 순수 도메인 모델로 구성
- **Infrastructure**: 기술 구현체. JPA Repository, Kafka Publisher 등

### 인프라 구성

```
                    ┌─────────────┐
                    │ commerce-api│
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    ┌─────┴─────┐   ┌─────┴─────┐   ┌─────┴─────┐
    │  MySQL 8.0 │   │   Redis    │   │   Kafka   │
    │            │   │  Master    │   │  (KRaft)  │
    └────────────┘   │  :6379    │   └─────┬─────┘
                     └─────┬─────┘         │
                     ┌─────┴─────┐   ┌─────┴──────────┐
                     │   Redis    │   │ commerce-      │
                     │  Replica   │   │ streamer       │
                     │  :6380    │   │ (Consumer)     │
                     └───────────┘   └────────────────┘
```

---

## Domain Model

```
User ──< Order ──< OrderItem >── Product >── Brand
User ──< Point (1:1)
User ──< Like >── Product
```

### 핵심 도메인

| 도메인 | 설명 |
|--------|------|
| **User** | 사용자 등록. 가입 시 `UserSignedUpEvent` 발행 → 포인트 자동 생성 |
| **Product** | 상품 CRUD. `Stock` Value Object로 재고 관리. 브랜드별 필터/정렬 지원 |
| **Order** | 주문/결제 처리. `StockDeductionService`로 복수 상품 재고를 원자적 차감 |
| **Point** | 포인트 조회/충전. `PointBalance` Value Object. 비관적 락 동시성 제어 |
| **Like** | 상품 좋아요 토글. 유니크 제약으로 중복 방지 |
| **Brand** | 브랜드 관리. 상품과 N:1 관계 |

### 도메인 이벤트

| 이벤트 | 트리거 | 소비자 |
|--------|--------|--------|
| `UserSignedUpEvent` | 회원가입 | `PointInitializationEventListener` → 포인트 자동 생성 |
| `OrderPlacedEvent` | 주문 완료 | Kafka → `commerce-streamer` |

---

## API Endpoints

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/users` | 사용자 등록 |
| `GET` | `/api/v1/points` | 포인트 조회 |
| `POST` | `/api/v1/points` | 포인트 충전 |
| `POST` | `/api/v1/products` | 상품 등록 |
| `GET` | `/api/v1/products/{id}` | 상품 상세 조회 |
| `GET` | `/api/v1/brands` | 브랜드 목록 조회 (필터/정렬) |
| `POST` | `/api/v1/orders` | 주문 생성 (`X-USER-ID` 헤더 필수) |
| `POST` | `/api/v1/likes` | 좋아요 추가 |
| `DELETE` | `/api/v1/likes` | 좋아요 제거 |

---

## Key Implementations

### 재고 차감 — Deadlock-Free 비관적 락

`StockDeductionService`는 복수 상품의 재고를 원자적으로 차감합니다.
데드락 방지를 위해 **Product ID 오름차순으로 비관적 락을 획득**합니다.

```java
// 1. ID 정렬 → 2. 비관적 락 획득 → 3. 재고 검증 → 4. 차감
List<Long> sortedProductIds = commands.stream()
        .map(StockDeductionCommand::productId)
        .sorted()
        .toList();
List<Product> products = productService.getProductsByIdsWithLock(sortedProductIds);
```

- `@Transactional(propagation = MANDATORY)` — 반드시 기존 트랜잭션 내에서 호출
- 차감 후 Redis 캐시 무효화 자동 수행

### 도메인 이벤트 기반 비동기 처리

주문 완료 시 `OrderPlacedEvent`를 Kafka로 발행하여 `commerce-streamer`에서 비동기 소비합니다.
Spring `ApplicationEventPublisher`로 도메인 이벤트를 발행하고, 인프라 레이어에서 Kafka로 전달하는 구조로 도메인-인프라 결합도를 제거했습니다.

### Redis Master-Replica 읽기 분산

- **Master** (:6379) — 쓰기 전용 (AOF 영속화)
- **Replica** (:6380) — 읽기 전용

### Value Object 기반 도메인 모델링

도메인 무결성을 Value Object 수준에서 보장합니다.

| Value Object | 검증 규칙 |
|--------------|-----------|
| `PointBalance` | 잔액 >= 0, 충전/차감 시 범위 검증 |
| `ProductPrice` | 가격 > 0 |
| `Stock` | 재고 >= 0, 차감 시 부족 여부 검증 |
| `OrderTotalAmount` | 주문 총액 계산 및 검증 |
| `Email` | 이메일 형식 검증 |
| `BirthDate` | 생년월일 범위 검증 |

### Soft Delete

`BaseEntity`에 `deletedAt` 필드를 두어 논리 삭제를 지원합니다.

---

## Testing

테스트 **131개** 전체 통과. 단위 테스트부터 E2E까지 계층별로 구성합니다.

| 계층 | 테스트 종류 | 예시 |
|------|------------|------|
| Domain | 단위 테스트 | `ProductTest`, `OrderTest`, `PointTest` |
| Domain | 통합 테스트 | `OrderServiceIntegrationTest`, `PointConcurrencyTest` |
| Domain | 동시성 테스트 | `StockConcurrencyTest`, `PointConcurrencyTest` |
| Domain | 제약 조건 테스트 | `LikeUniqueConstraintTest`, `SoftDeleteTest` |
| Infrastructure | 이벤트 테스트 | `OrderEventPublisherTest` |
| Interface | E2E 테스트 | `OrderV1ApiE2ETest`, `ProductV1ApiE2ETest` 등 6종 |

Testcontainers로 MySQL, Redis, Kafka를 테스트 환경에서 실제 구동하여 통합 테스트의 신뢰성을 보장합니다.

---

## Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose

### 인프라 실행

```bash
# MySQL + Redis Master/Replica + Kafka (KRaft)
docker-compose -f ./docker/infra-compose.yml up -d
```

### 애플리케이션 실행

```bash
./gradlew :apps:commerce-api:bootRun
```

### 모니터링 (Prometheus + Grafana)

```bash
docker-compose -f ./docker/monitoring-compose.yml up -d
```

Grafana: http://localhost:3000 (admin / admin)

### 테스트

```bash
./gradlew test
```

---

## Roadmap

| Phase | 주제 | 핵심 학습 포인트 |
|-------|------|-----------------|
| **1** | 미노출 API 연결 | 페이징 응답, 조건부 필드 (`isLiked`) |
| **2** | 주문 생명주기 + 포인트 이력 | 보상 트랜잭션, QueryDSL 동적 쿼리 |
| **3** | 장바구니 + 상품 관리 확장 | Redis Hash/Sorted Set, 캐시 무효화 전략 |
| **4** | 리뷰 + 쿠폰 + 알림 | 선착순 동시성(Redis DECR), Kafka Consumer, 계층형 모델 |
