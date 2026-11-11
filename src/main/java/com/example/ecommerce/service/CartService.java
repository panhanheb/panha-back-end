package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AddCartItemRequest;
import com.example.ecommerce.dto.response.CartResponse;
import com.example.ecommerce.dto.request.UpdateCartItemRequest;

/** Shopping cart of the currently authenticated customer. */
public interface CartService {

    CartResponse getCart();

    CartResponse addItem(AddCartItemRequest request);

    CartResponse updateItem(Long productId, UpdateCartItemRequest request);

    CartResponse removeItem(Long productId);

    CartResponse clear();
}
