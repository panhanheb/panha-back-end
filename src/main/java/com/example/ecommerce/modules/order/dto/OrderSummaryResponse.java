package com.example.ecommerce.modules.order.dto;

import com.example.ecommerce.modules.order.entity.OrderStatus;
import com.example.ecommerce.modules.order.entity.PaymentMethod;
import com.example.ecommerce.modules.order.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Row in an order list (without line items). */
public record OrderSummaryResponse(Long id,
                                   String orderNumber,
                                   Long customerId,
                                   String customerUsername,
                                   OrderStatus status,
                                   PaymentMethod paymentMethod,
                                   PaymentStatus paymentStatus,
                                   BigDecimal total,
                                   String currency,
                                   LocalDateTime createdAt) {
}
