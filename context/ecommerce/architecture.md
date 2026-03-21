# ecommerce 아키텍처

> 전체 구조 요약과 주제별 상세 문서 링크를 관리합니다.

## 시스템 구조

```
┌─────────────────────────────────────────────────┐
│                    apps/                         │
│  ┌──────────────┐     ┌───────────────────┐     │
│  │ commerce-api │     │ commerce-streamer  │     │
│  │  (REST API)  │     │  (Kafka Consumer)  │     │
│  └──────┬───────┘     └────────┬──────────┘     │
├─────────┼──────────────────────┼────────────────┤
│         │       modules/       │                 │
│  ┌──────┴───┐  ┌───────┐  ┌───┴────┐           │
│  │   jpa    │  │ redis  │  │ kafka  │           │
│  │ QueryDSL │  │ master │  │ batch  │           │
│  │  MySQL   │  │ replica│  │ manual │           │
│  └──────────┘  └────────┘  └────────┘           │
├─────────────────────────────────────────────────┤
│                  supports/                       │
│  ┌─────────┐  ┌─────────┐  ┌────────────┐      │
│  │ jackson  │  │ logging │  │ monitoring │      │
│  └─────────┘  └─────────┘  └────────────┘      │
└─────────────────────────────────────────────────┘

외부 인프라 (Docker Compose):
  MySQL 8.0 ─── Redis Master/Replica ─── Kafka (KRaft) ─── Prometheus/Grafana
```

## 레이어 구조 (Clean Architecture)

```
Interface Layer  → Controller, DTO, ApiSpec, ApiControllerAdvice
Application Layer → Facade, Command, Info
Domain Layer     → Entity, ValueObject, Service, Repository (interface)
Infrastructure   → RepositoryImpl, JpaRepository, Config
```

## 도메인 관계

```
User ──< Order ──< OrderItem >── Product >── Brand
User ──< Point (1:1)
User ──< Like >── Product
```

## 주제 문서

| 주제 | 설명 |
|------|------|
