package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import com.example.ecommerce.model.enums.OrderStatus;

/** Admin fulfilment step: SHIPPED, DELIVERED or CANCELLED. */
public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
