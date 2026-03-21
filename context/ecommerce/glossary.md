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
