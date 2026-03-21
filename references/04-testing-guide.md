# 테스트 작성 체크리스트

## 공통 규칙

- [ ] `@DisplayName` 한국어 서술형 ("유효한 정보로 상품을 생성할 수 있다.")
- [ ] `@Nested`로 메서드별 그룹핑
- [ ] AAA 패턴: `// arrange` → `// act` → `// assert`
- [ ] AssertJ 사용: `assertThat()`, `assertThatThrownBy()`, `assertAll()`

## 단위 테스트 체크리스트

파일: `src/test/java/.../domain/{entity}/{Entity}Test.java`

- [ ] Spring 컨텍스트 없음 (어노테이션 없는 순수 클래스)
- [ ] `@BeforeEach`로 공통 테스트 데이터 준비
- [ ] 정상 생성 테스트
- [ ] 검증 실패 테스트 — `@ParameterizedTest` + `@ValueSource`
- [ ] 예외 검증: `assertThatThrownBy().isInstanceOf(CoreException.class)`
- [ ] ErrorType 검증: `.extracting(ex -> ((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST)`

## 통합 테스트 체크리스트

파일: `src/test/java/.../domain/{entity}/{Entity}ServiceIntegrationTest.java`

- [ ] `@SpringBootTest`
- [ ] `@Autowired` Service, Repository
- [ ] `@Autowired DatabaseCleanUp` + `@AfterEach tearDown()` → `truncateAllTables()`
- [ ] `@BeforeEach`로 테스트 데이터 DB 저장
- [ ] 정상 조회 테스트 → `assertAll()` 그룹핑
- [ ] 미존재 조회 → `CoreException(NOT_FOUND)` 검증
- [ ] 쓰기 동작 테스트 (생성, 수정, 삭제)
- [ ] DB 상태 검증 (저장 후 재조회)

## E2E 테스트 체크리스트

파일: `src/test/java/.../interfaces/api/{resource}/{Resource}V1ApiE2ETest.java`

- [ ] `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- [ ] `@Autowired TestRestTemplate`
- [ ] `@Autowired DatabaseCleanUp` + `@AfterEach tearDown()`
- [ ] `ParameterizedTypeReference<ApiResponse<{ResponseDto}>>()` 사용
- [ ] `testRestTemplate.exchange(url, method, entity, typeRef)` 패턴
- [ ] 인증 헤더: `HttpHeaders headers = new HttpHeaders(); headers.set("X-USER-ID", "testuser");`

## 새 API 추가 시 필수 테스트 케이스

| 카테고리 | 테스트 | 확인 |
|----------|--------|------|
| 정상 | 성공 응답 + 데이터 검증 | - [ ] |
| 정상 | 빈 결과 (빈 배열/null) | - [ ] |
| 인증 | X-USER-ID 누락 → 401 | - [ ] |
| 입력 검증 | 잘못된 파라미터 → 400 | - [ ] |
| 미존재 | 없는 ID → 404 | - [ ] |
| 비즈니스 규칙 | 위반 시 → 409 | - [ ] |
| 경계값 | 최소/최대/빈 값 | - [ ] |
| 페이징 (목록) | page, size, totalElements 검증 | - [ ] |
| 정렬 (목록) | 각 sort 옵션 동작 검증 | - [ ] |
| 필터 (목록) | 필터 적용/미적용 검증 | - [ ] |
