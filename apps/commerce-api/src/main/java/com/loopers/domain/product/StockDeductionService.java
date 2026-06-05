package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockDeductionService {

    private final ProductService productService;
    private final RedisStockService redisStockService;

    // 부하테스트 Before/After 토글: false=비관적 락, true=Redis 원자 선차감
    @Value("${stock.redis.pre-decrement.enabled:false}")
    private boolean preDecrementEnabled;

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Product> deductStock(List<StockDeductionCommand> commands) {
        return preDecrementEnabled ? deductWithRedis(commands) : deductWithPessimisticLock(commands);
    }

    /**
     * [Before] 비관적 락 — ProductJpaRepository에서 ID 오름차순(ORDER BY p.id ASC)으로
     * SELECT ... FOR UPDATE 획득하여 데드락을 방지한다. 핫상품 동시 주문 시 직렬화 → 락 경합.
     */
    private List<Product> deductWithPessimisticLock(List<StockDeductionCommand> commands) {
        List<Long> sortedProductIds = commands.stream()
                .map(StockDeductionCommand::productId)
                .sorted()
                .toList();

        List<Product> products = productService.getProductsByIdsWithLock(sortedProductIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (StockDeductionCommand command : commands) {
            Product product = productMap.get(command.productId());
            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
            }
            if (!product.isStockAvailable(command.quantity())) {
                throw new CoreException(ErrorType.BAD_REQUEST,
                        String.format("상품 '%s'의 재고가 부족합니다.", product.getName()));
            }
            product.decreaseStock(command.quantity());
        }

        for (Long productId : sortedProductIds) {
            productService.evictProductCache(productId);
        }
        return products;
    }

    /**
     * [After] Redis 원자 선차감 — DECRBY로 재고 게이트를 통과(원자적)시킨 뒤
     * DB는 비관적 락 없이 차감만 반영한다. DB 락 경합을 제거해 처리량을 끌어올린다.
     */
    private List<Product> deductWithRedis(List<StockDeductionCommand> commands) {
        // 1) Redis 원자 선차감 (실패 시 이미 차감한 항목 롤백)
        List<StockDeductionCommand> deducted = new ArrayList<>();
        for (StockDeductionCommand command : commands) {
            if (!redisStockService.tryDeduct(command.productId(), command.quantity())) {
                for (StockDeductionCommand done : deducted) {
                    redisStockService.restore(done.productId(), done.quantity());
                }
                throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
            }
            deducted.add(command);
        }

        // 2) 재고의 진실 원천은 Redis. DB는 차감하지 않아(주문 트랜잭션에서 재고 UPDATE row lock 제거),
        //    주문 항목 생성에 필요한 상품 정보만 락 없이 조회한다.
        List<Long> ids = commands.stream()
                .map(StockDeductionCommand::productId).sorted().distinct().toList();
        List<Product> products = productService.getProductsByIds(ids);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (StockDeductionCommand command : commands) {
            if (productMap.get(command.productId()) == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
            }
        }
        return products;
    }

    public record StockDeductionCommand(Long productId, Integer quantity) {}
}
