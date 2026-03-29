# product 용어 사전

| 용어 | 설명 |
|------|------|
| StockDeductionService | 복수 상품 재고를 ID 오름차순 비관적 락으로 원자적 차감하는 도메인 서비스 |
| isLiked | (Phase 1) 로그인 사용자의 상품별 좋아요 여부 플래그 |
| Elasticsearch | (Phase 7) 역인덱스 기반 분산 검색 엔진. 상품 검색 전용 저장소로 도입 예정 |
| Nori | (Phase 7) Elasticsearch 한글 형태소 분석기 플러그인. 은전한닢 기반 |
| Inverted Index | (Phase 7) 역인덱스. 토큰 → 문서 ID 매핑으로 O(1) 검색. MySQL LIKE 풀스캔 대체 |
| Completion Suggester | (Phase 7) ES 자동완성 기능. Edge N-gram 토크나이저로 접두사 매칭 |
| Faceted Search | (Phase 7) 검색 결과에 대한 집계(Aggregation). 브랜드별/카테고리별/가격대별 상품 수 표시 |
| ProductIndexer | (Phase 7) MySQL → ES 인덱스 동기화 컴포넌트. Application Event(AFTER_COMMIT) 방식 |
| BM25 | (Phase 7) ES 기본 관련도 점수 알고리즘. TF-IDF 기반으로 검색어와 문서의 관련성 순위 산출 |
