package com.loopers.application.cart;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.cart.CartService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartFacade {

    private final CartService cartService;
    private final ProductService productService;
    private final OrderFacade orderFacade;

    public long addItem(String userId, Long productId, int quantity) {
        return cartService.addItem(userId, productId, quantity);
    }

    public void removeItem(String userId, Long productId) {
        cartService.removeItem(userId, productId);
    }

    public List<CartItemInfo> getCart(String userId) {
        Map<Long, Integer> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            return List.of();
        }

        // N+1 해소: productIds를 모아 일괄 조회, soft-delete된 상품은 결과에서 자연스럽게 제외
        List<Product> products = productService.findProductsByIds(cartItems.keySet());

        return products.stream()
                .map(product -> CartItemInfo.of(product, cartItems.get(product.getId())))
                .toList();
    }

    @Transactional
    public OrderInfo checkout(String userId) {
        Map<Long, Integer> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "장바구니가 비어 있습니다.");
        }

        List<OrderPlaceCommand.OrderItemCommand> itemCommands = cartItems.entrySet().stream()
                .map(entry -> new OrderPlaceCommand.OrderItemCommand(entry.getKey(), entry.getValue()))
                .toList();

        OrderPlaceCommand command = new OrderPlaceCommand(userId, itemCommands);
        OrderInfo orderInfo = orderFacade.placeOrder(command);

        // 주문 성공 후 장바구니 삭제 (Redis 작업이므로 RDB 트랜잭션에 포함되지 않음)
        try {
            cartService.clearCart(userId);
        } catch (Exception e) {
            log.warn("장바구니 삭제 실패 (주문은 정상 처리됨). userId={}", userId, e);
        }

        return orderInfo;
    }
}
