package com.loopers.interfaces.api.cart;

import com.loopers.application.cart.CartFacade;
import com.loopers.application.cart.CartItemInfo;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.SecurityContextHelper;
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
            @RequestBody @Valid CartV1Dto.AddItemRequest request
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        long currentQuantity = cartFacade.addItem(userId, request.productId(), request.quantity());
        return ApiResponse.success(new CartV1Dto.AddItemResponse(request.productId(), currentQuantity));
    }

    @DeleteMapping("/items/{productId}")
    @Override
    public ApiResponse<Void> removeItem(
            @PathVariable Long productId
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        cartFacade.removeItem(userId, productId);
        return ApiResponse.success(null);
    }

    @GetMapping
    @Override
    public ApiResponse<CartV1Dto.CartResponse> getCart() {
        String userId = SecurityContextHelper.getCurrentUserId();
        List<CartItemInfo> items = cartFacade.getCart(userId);
        return ApiResponse.success(CartV1Dto.CartResponse.from(items));
    }

    @PostMapping("/checkout")
    @Override
    public ApiResponse<CartV1Dto.CheckoutResponse> checkout() {
        String userId = SecurityContextHelper.getCurrentUserId();
        OrderInfo orderInfo = cartFacade.checkout(userId);
        return ApiResponse.success(CartV1Dto.CheckoutResponse.from(orderInfo));
    }
}
