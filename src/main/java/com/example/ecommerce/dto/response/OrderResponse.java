package com.example.ecommerce.dto.response;

import com.example.ecommerce.model.enums.OrderStatus;
import com.example.ecommerce.model.enums.PaymentMethod;
import com.example.ecommerce.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id,
                            String orderNumber,
                            Long customerId,
                            String customerUsername,
                            OrderStatus status,
                            List<OrderItemResponse> items,
                            BigDecimal subtotal,
                            BigDecimal shippingFee,
                            BigDecimal total,
                            String currency,
                            ShippingAddress shippingAddress,
                            String note,
                            PaymentMethod paymentMethod,
                            PaymentStatus paymentStatus,
                            String paymentReference,
                            LocalDateTime paidAt,
                            LocalDateTime shippedAt,
                            LocalDateTime deliveredAt,
                            LocalDateTime cancelledAt,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {

    public record ShippingAddress(String recipientName,
                                  String phone,
                                  String addressLine,
                                  String city,
                                  String postalCode,
                                  String country) {
    }
}
