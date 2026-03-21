# ecommerce 용어 사전

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
