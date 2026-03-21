package com.loopers.domain.cart;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public long addItem(String userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.");
        }
        long currentQuantity = cartRepository.addItem(userId, productId, quantity);
        cartRepository.refreshTtl(userId);
        return currentQuantity;
    }

    public void removeItem(String userId, Long productId) {
        if (!cartRepository.existsItem(userId, productId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "장바구니에 해당 상품이 없습니다.");
        }
        cartRepository.removeItem(userId, productId);
    }

    public Map<Long, Integer> getCartItems(String userId) {
        return cartRepository.getCartItems(userId);
    }

    public void clearCart(String userId) {
        cartRepository.deleteCart(userId);
    }
}
