# ecommerce 용어 사전

> 프로젝트 공통 + 운영/인프라 용어입니다.
> 도메인별 용어는 `context/{도메인}/glossary.md`를 참조하세요.

## 프로젝트 공통

| 용어 | 설명 |
|------|------|
| commerce-api | REST API 애플리케이션 (port 8080) |
| commerce-streamer | Kafka consumer 애플리케이션 |
| Facade | 트랜잭션 경계를 관리하는 애플리케이션 레이어 오케스트레이터 |
| Command | 입력 DTO (Application Layer) |
| Info | 출력 DTO (Application Layer) |
| CoreException | 프로젝트 공통 커스텀 예외 (ErrorType 기반) |
| ErrorType | HTTP 상태 코드 매핑 열거형 (BAD_REQUEST, NOT_FOUND, CONFLICT, INTERNAL_ERROR) |
| BaseEntity | 공통 엔티티 (id, createdAt, updatedAt, deletedAt 포함, soft delete 지원) |
| KRaft | Kafka의 ZooKeeper 없는 합의 프로토콜 모드 |
| modules/ | 재사용 가능한 인프라 설정 모듈 (jpa, redis, kafka) |
| supports/ | 부가 유틸리티 모듈 (jackson, logging, monitoring) |

## 운영/인프라

| 용어 | 설명 |
|------|------|
| DistributedLockService | (Phase 5) Redisson 기반 분산 락 서비스. 쿠폰 발급 등 동시성 제어에 사용 |
| RateLimitInterceptor | (Phase 5) Redis Sliding Window 기반 API 요청 제한 (분당 60회) |
| CursorPageResponse | (Phase 5) Cursor 기반 페이지네이션 응답. offset 방식 대비 대량 데이터에 유리 |
| CircuitBreaker | (Phase 5) Resilience4j 기반. Kafka/Redis 장애 시 빠른 실패 처리 |
| RedisHealthIndicator | (Phase 5) Redis 커넥션 상태를 모니터링하는 커스텀 헬스체크 |
