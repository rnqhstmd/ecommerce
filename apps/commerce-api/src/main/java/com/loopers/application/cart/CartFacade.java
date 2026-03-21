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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        List<CartItemInfo> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cartItems.entrySet()) {
            try {
                Product product = productService.getProduct(entry.getKey());
                result.add(CartItemInfo.of(product, entry.getValue()));
            } catch (CoreException e) {
                // soft-delete된 상품은 응답에서 제외 (Redis 항목은 유지)
                if (e.getErrorType() == ErrorType.NOT_FOUND) {
                    continue;
                }
                throw e;
            }
        }
        return result;
    }

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

        // 주문 성공 후 장바구니 삭제
        cartService.clearCart(userId);

        return orderInfo;
    }
}
