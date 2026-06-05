-- 부하테스트 시드 데이터
-- 실제 스키마 기준: products(name,price,stock,like_count,brand_id,created_at,updated_at), brands(name,...)
-- 재고 컬럼명은 stock(int), 테이블명은 복수형.
-- 핫상품 = products.id 1~10 (재고 넉넉히 100000 → 품절 아닌 '락 경합' 측정이 목적)

SET SESSION cte_max_recursion_depth = 20000;

-- 브랜드 10개 (id 1~10)
INSERT INTO brands (name, created_at, updated_at)
WITH RECURSIVE seq(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10
)
SELECT CONCAT('brand-', n), NOW(), NOW() FROM seq;

-- 상품 10,000개 (brand_id 1~10 분산, 재고 100000)
INSERT INTO products (name, price, stock, like_count, brand_id, created_at, updated_at)
WITH RECURSIVE seq(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10000
)
SELECT CONCAT('product-', n),
       1000 + (n % 100) * 10,   -- 가격 1000~1990
       100000,                  -- 재고 충분
       0,                       -- like_count
       1 + (n % 10),            -- brand_id 1~10
       NOW(), NOW()
FROM seq;
