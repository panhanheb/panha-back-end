package com.example.ecommerce.dto.request;

import com.example.ecommerce.util.CartLimits;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Sets the quantity of a product already in the cart. */
public record UpdateCartItemRequest(@NotNull @Min(1) @Max(CartLimits.MAX_QUANTITY_PER_ITEM) Integer quantity) {
}
