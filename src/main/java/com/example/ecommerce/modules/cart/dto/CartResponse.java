package com.example.ecommerce.modules.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param totalQuantity sum of all item quantities
 * @param checkoutReady true when the cart is non-empty and every item is available
 */
public record CartResponse(List<CartItemResponse> items,
                           int totalQuantity,
                           BigDecimal subtotal,
                           BigDecimal shippingFee,
                           BigDecimal total,
                           String currency,
                           boolean checkoutReady) {
}
