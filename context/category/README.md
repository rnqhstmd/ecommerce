# category

계층형 카테고리 트리를 관리하는 도메인. parentId self-reference로 다단계 구조 표현.

## 핵심 엔티티

| 엔티티 | 필드 | 설명 |
|--------|------|------|
| Category | name, parentId, depth | 계층형 카테고리. parentId self-reference, depth로 깊이 추적 |

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/v1/categories | 카테고리 등록 (루트 또는 하위) | ADMIN |
| GET | /api/v1/categories | 전체 카테고리 목록 | 불필요 |

## 관련 Phase

| Phase | 관련 기능 |
|-------|----------|
| Phase 4 | 계층형 카테고리 모델링, Product에 categoryId 추가 |
| Phase 6 | 카테고리 등록 ADMIN 전용 |

## 도메인 관계

- **product**: Product.categoryId FK (nullable). 상품은 하나의 카테고리에 속할 수 있음
