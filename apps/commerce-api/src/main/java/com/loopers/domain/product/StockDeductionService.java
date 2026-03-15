package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockDeductionService {

    private final ProductService productService;

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

        return products;
    }

    public record StockDeductionCommand(Long productId, Integer quantity) {}
}
