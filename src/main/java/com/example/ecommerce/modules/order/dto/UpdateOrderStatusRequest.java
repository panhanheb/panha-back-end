package com.example.ecommerce.modules.order.dto;

import jakarta.validation.constraints.NotNull;
import com.example.ecommerce.modules.order.entity.OrderStatus;

/** Admin fulfilment step: SHIPPED, DELIVERED or CANCELLED. */
public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
