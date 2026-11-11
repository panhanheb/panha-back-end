package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Sets the absolute stock level of a product (e.g. after a stock count or a delivery). */
public record StockUpdateRequest(@NotNull @Min(0) Integer stockQuantity) {
}
