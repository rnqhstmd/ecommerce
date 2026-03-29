# like

사용자의 상품 좋아요 추가/제거/목록을 담당하는 도메인.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| Like | userId, productId | 좋아요. (userId, productId) 유니크 제약 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/likes | 좋아요 추가 | USER |
| DELETE | /api/v1/likes/{productId} | 좋아요 제거 | USER |
| GET | /api/v1/likes | 내 좋아요 목록 | USER |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Week 2 | 좋아요 추가/제거/목록, 유니크 제약 테스트 |
| Phase 1 | 상품 상세 isLiked 플래그 |

## 도메인 관계

- **product**: 좋아요 추가/제거 시 Product.likeCount 증감 (비정규화)
- **user**: userId 참조
