# brand

상품이 속하는 브랜드를 관리하는 도메인.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| Brand | name | 브랜드. 이름 유니크 제약, 50자 이하 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/brands | 브랜드 등록 | ADMIN |
| GET | /api/v1/brands/{id} | 브랜드 단건 조회 | 불필요 |
| GET | /api/v1/brands | 브랜드 목록 (페이징) | 불필요 |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Week 2 | 브랜드 조회 |
| Phase 1 | 브랜드 단건 조회 API |
| Phase 3 | 브랜드 목록 페이징, Spring Cache (1h TTL) |
| Phase 6 | 브랜드 등록 ADMIN 전용 |

## 도메인 관계

- **product**: Product.brandId FK. 상품은 반드시 하나의 브랜드에 속함
