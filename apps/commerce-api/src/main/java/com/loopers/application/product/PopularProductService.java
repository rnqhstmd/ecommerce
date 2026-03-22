package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularProductService {

    private static final String POPULAR_KEY = "product:popular";
    private static final long CACHE_TTL_HOURS = 1;

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductService productService;

    @Retry(name = "redisRetry", fallbackMethod = "getPopularProductsFallback")
    public List<PopularProductInfo> getPopularProducts(int limit) {
        // 캐시 히트 시 Redis에서 반환
        Set<ZSetOperations.TypedTuple<String>> cached = redisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_KEY, 0, limit - 1);

        if (cached != null && !cached.isEmpty()) {
            List<Long> productIds = cached.stream()
                    .map(t -> Long.parseLong(Objects.requireNonNull(t.getValue())))
                    .toList();
            Map<Long, Double> scoreMap = cached.stream()
                    .collect(Collectors.toMap(
                            t -> Long.parseLong(Objects.requireNonNull(t.getValue())),
                            t -> t.getScore() != null ? t.getScore() : 0.0
                    ));

            // DB에서 상품 정보 조회 (삭제된 상품은 결과에서 자연스럽게 제외)
            List<Product> products = productService.findProductsByIds(productIds);

            return products.stream()
                    .map(p -> PopularProductInfo.of(p, scoreMap.getOrDefault(p.getId(), 0.0).longValue()))
                    .sorted(Comparator.comparingLong(PopularProductInfo::likeCount).reversed())
                    .toList();
        }

        // 캐시 미스 시 DB에서 조회 후 Redis에 적재
        List<Product> topProducts = productService.findTopByLikeCountDesc(limit);
        if (topProducts.isEmpty()) {
            return List.of();
        }

        // Redis Sorted Set에 적재
        Set<ZSetOperations.TypedTuple<String>> tuples = topProducts.stream()
                .map(p -> ZSetOperations.TypedTuple.of(
                        p.getId().toString(),
                        (double) p.getLikeCount()
                ))
                .collect(Collectors.toSet());

        redisTemplate.opsForZSet().add(POPULAR_KEY, tuples);
        redisTemplate.expire(POPULAR_KEY, CACHE_TTL_HOURS, TimeUnit.HOURS);

        return toPopularProductInfos(topProducts);
    }

    private List<PopularProductInfo> getPopularProductsFallback(int limit, Throwable t) {
        log.warn("인기 상품 조회 Redis 장애 발생, DB 직접 조회로 폴백: {}", t.getMessage());
        List<Product> topProducts = productService.findTopByLikeCountDesc(limit);
        return toPopularProductInfos(topProducts);
    }

    private List<PopularProductInfo> toPopularProductInfos(List<Product> products) {
        return products.stream()
                .map(p -> PopularProductInfo.of(p, p.getLikeCount()))
                .toList();
    }
}
