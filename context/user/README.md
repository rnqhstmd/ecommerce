# user

회원가입, 로그인, 내 정보 조회 등 사용자 계정과 인증/인가를 담당하는 도메인.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| User | userId, email, birthDate, gender, password, role | 사용자 계정. Role(USER/ADMIN) 기반 RBAC |
| Email | value | 이메일 Value Object |
| BirthDate | value | 생년월일 Value Object |
| Role | USER, ADMIN | 역할 열거형 |
| Gender | - | 성별 열거형 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/users | 회원가입 (레거시) | 불필요 |
| GET | /api/v1/users/me | 내 정보 조회 | USER |
| POST | /api/v1/auth/signup | 회원가입 (JWT) | 불필요 |
| POST | /api/v1/auth/login | 로그인 | 불필요 |
| POST | /api/v1/auth/refresh | 토큰 갱신 | 불필요 |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Week 2 | 사용자 등록 (POST /users) |
| Phase 6 | Spring Security + JWT 인증/인가, RBAC, BCrypt 비밀번호 암호화 |

## 도메인 관계

- **point**: UserSignedUpEvent 발행 시 PointInitializationEventListener가 포인트 자동 생성
- **order**: 주문 생성 시 userId 참조
- **like**: 좋아요 생성 시 userId 참조
- **review**: 리뷰 작성 시 userId 참조
- **cart**: 장바구니 Redis Hash 키로 userId 사용
- **coupon**: 쿠폰 발급 시 userId 참조
