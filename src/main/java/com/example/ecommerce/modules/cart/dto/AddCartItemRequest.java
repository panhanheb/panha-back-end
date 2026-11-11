package com.example.ecommerce.modules.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Adds a product to the cart; if it is already there, the quantity is increased. */
public record AddCartItemRequest(@NotNull Long productId,
                                 @NotNull @Min(1) @Max(CartLimits.MAX_QUANTITY_PER_ITEM) Integer quantity) {
}
