package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockDeductionService {

    private final ProductService productService;

    /**
     * 재고 차감 도메인 서비스.
     * 반드시 기존 트랜잭션 내에서 호출되어야 합니다 (MANDATORY).
     * 비관적 락은 ProductJpaRepository에서 ID 오름차순(ORDER BY p.id ASC)으로 획득하여 데드락을 방지합니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Product> deductStock(List<StockDeductionCommand> commands) {
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

        // 변경된 상품에 대한 캐시 무효화
        for (Long productId : sortedProductIds) {
            productService.evictProductCache(productId);
        }

        return products;
    }

    public record StockDeductionCommand(Long productId, Integer quantity) {}
}
