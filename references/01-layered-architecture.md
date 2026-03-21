# 레이어 아키텍처 체크리스트

## 의존 방향

- [ ] Controller → Facade만 호출 (Service 직접 호출 금지)
- [ ] Facade → Service만 호출 (Repository 직접 호출 금지)
- [ ] Service → Repository만 주입 (다른 Service 주입 시 순환 의존 주의)
- [ ] RepositoryImpl → JpaRepository 위임

## Controller 작성 체크리스트

- [ ] `@RestController` + `@RequestMapping("/api/v1/{resource복수}")` + `@RequiredArgsConstructor`
- [ ] `implements {Resource}V1ApiSpec`
- [ ] Facade만 주입 (`private final {Entity}Facade facade`)
- [ ] 모든 응답 `ApiResponse.success(data)` 래핑
- [ ] `@RequestBody @Valid` — 요청 바디 검증
- [ ] 인증 필수 API: `@RequestHeader(value = "X-USER-ID", required = false)` → null/blank 시 `CoreException(UNAUTHORIZED)`
- [ ] 쿼리 파라미터 범위 검증은 Controller에서 수행 (page, size, sort 등)
- [ ] 메서드에 `@Override` (ApiSpec 구현)

## ApiSpec 작성 체크리스트

- [ ] `@Tag(name = "{Resource} API", description = "...")` 클래스 레벨
- [ ] 각 메서드에 `@Operation(summary, description)`
- [ ] `@ApiResponses` — 200, 400, 401, 404 등 가능한 응답 코드
- [ ] 파라미터에 `@Parameter(description = "...", required = true/false)`

## DTO 작성 체크리스트

- [ ] Java `record` 사용 (불변)
- [ ] 관련 record를 하나의 `{Resource}V1Dto` 클래스에 중첩
- [ ] Request: 검증 어노테이션 + 한국어 message (`@NotBlank(message = "이름은 필수입니다.")`)
- [ ] Response: `public static {Response} from({Info} info)` 정적 팩토리
- [ ] 목록 응답: `{Resource}ListResponse(List<ContentResponse> contents, int page, int size, long totalElements, int totalPages)`

## Facade 작성 체크리스트

- [ ] `@Component` + `@RequiredArgsConstructor` + `@Transactional(readOnly = true)` 클래스 레벨
- [ ] 쓰기 메서드에만 `@Transactional` 오버라이드
- [ ] 여러 Service 조합 → Info 객체 반환 (엔티티 직접 반환 금지)
- [ ] 하위 호환 필요 시 오버로드 메서드 유지

## Command / Info 작성 체크리스트

- [ ] Java `record` 사용
- [ ] Command: Controller → Facade 입력 데이터
- [ ] Info: `public static {Info} of(Entity entity, ...)` 정적 팩토리
- [ ] Info의 필드는 원시 타입으로 평탄화 (VO.getValue() 사용)

## Service 작성 체크리스트

- [ ] `@Service` + `@RequiredArgsConstructor` + `@Transactional(readOnly = true)` 클래스 레벨
- [ ] Repository만 주입
- [ ] 조회 실패 → `CoreException(ErrorType.NOT_FOUND, "한국어 메시지")`
- [ ] 쓰기 메서드에 `@Transactional` 오버라이드
- [ ] 캐시 대상이면 `@Cacheable` / `@CacheEvict`

## Repository 3계층 체크리스트

### Domain (interface)
- [ ] Spring 어노테이션 없음
- [ ] 도메인 객체만 반환 (`Entity`, `Optional<Entity>`, `List<Entity>`, `Page<Entity>`)

### Infrastructure (Impl)
- [ ] `@Repository` + `@RequiredArgsConstructor`
- [ ] `implements {Entity}Repository`
- [ ] JpaRepository에 위임만 수행 (로직 없음)

### Infrastructure (JPA)
- [ ] `extends JpaRepository<Entity, Long>`
- [ ] JPQL: `@Query("SELECT ...")` + `@Param`
- [ ] 비관적 락: `@Lock(PESSIMISTIC_WRITE)` + `ORDER BY e.id ASC`
- [ ] 페이징: `Pageable` 파라미터 + `Page<Entity>` 반환
- [ ] Null-safe 필터: `(:param IS NULL OR e.field = :param)`
