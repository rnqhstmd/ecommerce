package com.loopers.interfaces.api.cart;

import com.loopers.application.cart.CartFacade;
import com.loopers.application.cart.CartItemInfo;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartV1Controller implements CartV1ApiSpec {

    private final CartFacade cartFacade;

    @PostMapping("/items")
    @Override
    public ApiResponse<CartV1Dto.AddItemResponse> addItem(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestBody @Valid CartV1Dto.AddItemRequest request
    ) {
        validateUserId(userId);
        long currentQuantity = cartFacade.addItem(userId, request.productId(), request.quantity());
        return ApiResponse.success(new CartV1Dto.AddItemResponse(request.productId(), currentQuantity));
    }

    @DeleteMapping("/items/{productId}")
    @Override
    public ApiResponse<Void> removeItem(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PathVariable Long productId
    ) {
        validateUserId(userId);
        cartFacade.removeItem(userId, productId);
        return ApiResponse.success(null);
    }

    @GetMapping
    @Override
    public ApiResponse<CartV1Dto.CartResponse> getCart(
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        validateUserId(userId);
        List<CartItemInfo> items = cartFacade.getCart(userId);
        return ApiResponse.success(CartV1Dto.CartResponse.from(items));
    }

    @PostMapping("/checkout")
    @Override
    public ApiResponse<CartV1Dto.CheckoutResponse> checkout(
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        validateUserId(userId);
        OrderInfo orderInfo = cartFacade.checkout(userId);
        return ApiResponse.success(CartV1Dto.CheckoutResponse.from(orderInfo));
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }
    }
}
