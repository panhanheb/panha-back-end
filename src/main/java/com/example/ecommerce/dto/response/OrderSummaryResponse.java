package com.example.ecommerce.dto.response;

import com.example.ecommerce.model.enums.OrderStatus;
import com.example.ecommerce.model.enums.PaymentMethod;
import com.example.ecommerce.model.enums.PaymentStatus;

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
