package com.example.ecommerce.modules.cart.service;

import com.example.ecommerce.modules.cart.dto.AddCartItemRequest;
import com.example.ecommerce.modules.cart.dto.CartResponse;
import com.example.ecommerce.modules.cart.dto.UpdateCartItemRequest;

/** Shopping cart of the currently authenticated customer. */
public interface CartService {

    CartResponse getCart();

    CartResponse addItem(AddCartItemRequest request);

    CartResponse updateItem(Long productId, UpdateCartItemRequest request);

    CartResponse removeItem(Long productId);

    CartResponse clear();
}
