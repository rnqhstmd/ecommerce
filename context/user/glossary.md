# user 용어 사전

| 용어 | 설명 |
|------|------|
| UserSignedUpEvent | 회원가입 시 발행. PointInitializationEventListener가 수신하여 포인트 자동 생성 |
| JwtTokenProvider | (Phase 6) Access Token(30분) / Refresh Token(7일, Redis 저장) 생성·검증 |
| JwtAuthenticationFilter | (Phase 6) Bearer 토큰을 파싱하여 SecurityContext에 인증 정보 설정 |
| SecurityContextHelper | (Phase 6) SecurityContext에서 현재 사용자 ID를 추출하는 유틸. X-USER-ID 헤더 대체 |
| RBAC | (Phase 6) Role-Based Access Control. USER/ADMIN 역할로 엔드포인트 접근 제어 |
| TestAuthHelper | (Phase 6) E2E 테스트에서 JWT 토큰 발급을 지원하는 테스트 유틸리티 |
