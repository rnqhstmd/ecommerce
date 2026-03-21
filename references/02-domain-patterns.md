# 도메인 모델 체크리스트

## Entity 작성 체크리스트

- [ ] `@Entity` + `@Getter` + `@Table(name = "복수형")` + `@NoArgsConstructor(access = PROTECTED)`
- [ ] `@SQLRestriction("deleted_at IS NULL")` — 소프트 삭제 필터
- [ ] `extends BaseEntity` — id, createdAt, updatedAt, deletedAt 상속
- [ ] 생성자 `private` — 외부 직접 생성 차단
- [ ] `public static {Entity} create(...)` — 유일한 생성 경로
- [ ] 생성자 내부에서 `validate()` 호출
- [ ] Cross-aggregate 참조는 **ID만** (`Long brandId`, 엔티티 참조 X)
- [ ] VO 필드는 `@Embedded` 사용
- [ ] VO 값 접근 헬퍼: `getVoValue()` → `this.vo.getValue()`
- [ ] 비즈니스 메서드는 불변 패턴: `this.vo = this.vo.modify(...)` (새 객체 할당)

## Value Object 작성 체크리스트

- [ ] `@Getter` + `@Embeddable` + `@EqualsAndHashCode` + `@NoArgsConstructor(access = PROTECTED)`
- [ ] 단일 `value` 필드 + `@Column(name = "...", nullable = false)`
- [ ] 생성자 `private` + `validate()` 호출
- [ ] `public static {VO} of({Type} value)` — 팩토리 메서드
- [ ] null 검증 + 범위 검증 → `CoreException(BAD_REQUEST)`
- [ ] 연산 메서드는 **새 인스턴스 반환** (기존 객체 변경 금지)
- [ ] 오버플로우 체크 필요 시 `Math.addExact()` 등 사용

## BaseEntity 상속 확인

- [ ] `id` (Long, IDENTITY) — 자동 생성
- [ ] `createdAt` (ZonedDateTime) — @PrePersist 자동
- [ ] `updatedAt` (ZonedDateTime) — @PrePersist/@PreUpdate 자동
- [ ] `deletedAt` (ZonedDateTime, nullable) — `delete()` 호출 시 설정
- [ ] `delete()` / `restore()` — 멱등 동작

## 도메인 이벤트 체크리스트

- [ ] `public record {Entity}{Action}Event(...)` — 불변 레코드
- [ ] 스냅샷 데이터 포함 (이벤트 발생 시점 상태 캡처)
- [ ] 발행: `applicationEventPublisher.publishEvent(event)`
- [ ] 수신: `@TransactionalEventListener(phase = BEFORE_COMMIT 또는 AFTER_COMMIT)`
- [ ] BEFORE_COMMIT: 같은 트랜잭션 내 처리 (포인트 생성 등)
- [ ] AFTER_COMMIT: 외부 시스템 연동 (Kafka 발행 등)

## Aggregate 설계 체크리스트

- [ ] Aggregate Root만 Repository를 갖는다
- [ ] Aggregate 내부 엔티티는 Root를 통해서만 접근 (OrderItem → Order)
- [ ] 내부 엔티티 Cascade: `@OneToMany(cascade = ALL, orphanRemoval = true)`
- [ ] Cross-aggregate: ID 참조 + 도메인 이벤트로 통신
- [ ] 비정규화 필드 사용 시 갱신 경로 명확히 (likeCount ↔ addLike/removeLike)

## 동시성 제어 체크리스트

- [ ] 재고 차감: 비관적 락 + ID 오름차순 정렬 (데드락 방지)
- [ ] 포인트 사용/충전: 비관적 락
- [ ] 교차 엔티티 락 순서 고정: Product → Point
- [ ] 유니크 제약 위반 가능성 → `DataIntegrityViolationException` 처리 고려
