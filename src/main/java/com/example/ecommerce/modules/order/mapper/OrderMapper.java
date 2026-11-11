package com.example.ecommerce.modules.order.mapper;

import com.example.ecommerce.modules.order.dto.OrderItemResponse;
import com.example.ecommerce.modules.order.dto.OrderResponse;
import com.example.ecommerce.modules.order.dto.OrderSummaryResponse;
import com.example.ecommerce.modules.order.entity.Order;
import com.example.ecommerce.modules.order.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                order.getStatus(),
                order.getItems().stream().map(this::toResponse).toList(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotal(),
                order.getCurrency(),
                new OrderResponse.ShippingAddress(
                        order.getRecipientName(),
                        order.getPhone(),
                        order.getAddressLine(),
                        order.getCity(),
                        order.getPostalCode(),
                        order.getCountry()),
                order.getNote(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getPaymentReference(),
                order.getPaidAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getTotal(),
                order.getCurrency(),
                order.getCreatedAt());
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getSku(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal());
    }
}
