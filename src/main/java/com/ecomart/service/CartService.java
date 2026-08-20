package com.ecomart.service;

import com.ecomart.dto.request.AddToCartRequest;
import com.ecomart.dto.request.UpdateCartItemRequest;
import com.ecomart.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request);

    void removeCartItem(Long userId, Long cartItemId);

    void clearCart(Long userId);
}
