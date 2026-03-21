# 에러 처리 체크리스트

## ErrorType 선택 가이드

| 상황 | ErrorType | HTTP |
|------|-----------|------|
| 입력 검증 실패 (형식, 범위) | `BAD_REQUEST` | 400 |
| X-USER-ID 헤더 누락/공백 | `UNAUTHORIZED` | 401 |
| 엔티티 미존재 | `NOT_FOUND` | 404 |
| 중복 리소스, 재고/포인트 부족 | `CONFLICT` | 409 |
| 예상치 못한 오류 | `INTERNAL_ERROR` | 500 |

## 예외 발생 체크리스트

- [ ] 조회 실패: `repository.findById(id).orElseThrow(() -> new CoreException(NOT_FOUND, "한국어"))`
- [ ] 입력 검증: `if (invalid) throw new CoreException(BAD_REQUEST, "한국어")`
- [ ] 인증 검증: `if (userId == null || userId.isBlank()) throw new CoreException(UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.")`
- [ ] 비즈니스 규칙: `if (balance < amount) throw new CoreException(CONFLICT, "포인트가 부족합니다.")`

## 에러 메시지 규칙

- [ ] 한국어로 작성
- [ ] 사용자 입력값을 메시지에 포함하지 않음 (정보 노출 방지)
- [ ] 허용 값 안내 시 고정 문자열: `"sort는 latest, price_asc, likes_desc 중 하나여야 합니다."`

## API 응답 구조

```json
성공: { "meta": { "result": "SUCCESS", "errorCode": null, "message": null }, "data": { ... } }
실패: { "meta": { "result": "FAIL", "errorCode": "NOT_FOUND", "message": "..." }, "data": null }
```

- [ ] 성공: `ApiResponse.success(data)`
- [ ] 실패: `ApiResponse.fail(errorType.getCode(), message)`

## 검증 위치 체크리스트

| 검증 유형 | 위치 | 확인 |
|-----------|------|------|
| HTTP body 형식 | DTO `@Valid` | - [ ] |
| 쿼리 파라미터 범위 | Controller | - [ ] |
| 인증 헤더 | Controller | - [ ] |
| 엔티티 존재 여부 | Service | - [ ] |
| 도메인 불변식 | Entity/VO 생성자 | - [ ] |
| 비즈니스 규칙 | Entity/VO 메서드 | - [ ] |
