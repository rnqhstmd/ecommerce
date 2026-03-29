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
| StockDeductionService | 복수 상품 재고를 ID 오름차순 비관적 락으로 원자적 차감하는 도메인 서비스 |
| OrderPlacedEvent | 주문 완료 시 발행되는 도메인 이벤트. Kafka로 전송 |
| UserSignedUpEvent | 회원가입 시 발행. PointInitializationEventListener가 수신하여 포인트 자동 생성 |
| isLiked | (Phase 1) 로그인 사용자의 상품별 좋아요 여부 플래그 |
| 보상 트랜잭션 | (Phase 2) 주문 취소 시 재고 복구 + 포인트 환불을 원자적으로 처리하는 패턴 |
| PointHistory | (Phase 2) 포인트 충전/사용/환불 이력을 기록하는 엔티티 |
| Cart | (Phase 3) Redis Hash 기반 장바구니. TTL 7일. 주문 전환 시 삭제 |
| CouponPolicy | (Phase 4) 쿠폰 정책 엔티티. 할인율/금액, 유효기간, 발급수량 한도 |
| UserCoupon | (Phase 4) 사용자별 쿠폰 발급 이력. 선착순 동시성 제어 대상 |
| Review | (Phase 4) 구매 확인 후 작성 가능한 상품 리뷰. rating(1~5) + content |
| Category | (Phase 4) 계층형 카테고리. parentId self-reference |
| DistributedLockService | (Phase 5) Redisson 기반 분산 락 서비스. 쿠폰 발급 등 동시성 제어에 사용 |
| RateLimitInterceptor | (Phase 5) Redis Sliding Window 기반 API 요청 제한 (분당 60회) |
| CursorPageResponse | (Phase 5) Cursor 기반 페이지네이션 응답. offset 방식 대비 대량 데이터에 유리 |
| CircuitBreaker | (Phase 5) Resilience4j 기반. Kafka/Redis 장애 시 빠른 실패 처리 |
| RedisHealthIndicator | (Phase 5) Redis 커넥션 상태를 모니터링하는 커스텀 헬스체크 |
| JwtTokenProvider | (Phase 6) Access Token(30분) / Refresh Token(7일, Redis 저장) 생성·검증 |
| JwtAuthenticationFilter | (Phase 6) Bearer 토큰을 파싱하여 SecurityContext에 인증 정보 설정 |
| SecurityContextHelper | (Phase 6) SecurityContext에서 현재 사용자 ID를 추출하는 유틸. X-USER-ID 헤더 대체 |
| RBAC | (Phase 6) Role-Based Access Control. USER/ADMIN 역할로 엔드포인트 접근 제어 |
| TestAuthHelper | (Phase 6) E2E 테스트에서 JWT 토큰 발급을 지원하는 테스트 유틸리티 |
| Elasticsearch | (Phase 7) 역인덱스 기반 분산 검색 엔진. 상품 검색 전용 저장소로 도입 예정 |
| Nori | (Phase 7) Elasticsearch 한글 형태소 분석기 플러그인. 은전한닢 기반 |
| Inverted Index | (Phase 7) 역인덱스. 토큰 → 문서 ID 매핑으로 O(1) 검색. MySQL LIKE 풀스캔 대체 |
| Completion Suggester | (Phase 7) ES 자동완성 기능. Edge N-gram 토크나이저로 접두사 매칭 |
| Faceted Search | (Phase 7) 검색 결과에 대한 집계(Aggregation). 브랜드별/카테고리별/가격대별 상품 수 표시 |
| ProductIndexer | (Phase 7) MySQL → ES 인덱스 동기화 컴포넌트. Application Event(AFTER_COMMIT) 방식 |
| BM25 | (Phase 7) ES 기본 관련도 점수 알고리즘. TF-IDF 기반으로 검색어와 문서의 관련성 순위 산출 |
