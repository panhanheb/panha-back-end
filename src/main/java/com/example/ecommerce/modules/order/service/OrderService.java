package com.example.ecommerce.modules.order.service;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.modules.order.dto.CheckoutRequest;
import com.example.ecommerce.modules.order.dto.OrderResponse;
import com.example.ecommerce.modules.order.dto.OrderSummaryResponse;
import com.example.ecommerce.modules.order.dto.PaymentRequest;
import com.example.ecommerce.modules.order.dto.UpdateOrderStatusRequest;
import com.example.ecommerce.modules.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    /** Turns the current customer's cart into an order, reserving stock, and empties the cart. */
    OrderResponse checkout(CheckoutRequest request);

    /** Simulated online payment of a PENDING card / bank-transfer order by its owner. */
    OrderResponse pay(Long orderId, PaymentRequest request);

    /** Cancels a PENDING or PAID order (owner or admin), restoring stock and refunding if paid. */
    OrderResponse cancel(Long orderId);

    /** Admin fulfilment: ship, deliver or cancel. */
    OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request);

    /** Owner or admin. */
    OrderResponse getById(Long orderId);

    PageResponse<OrderSummaryResponse> myOrders(OrderStatus status, Pageable pageable);

    PageResponse<OrderSummaryResponse> search(OrderStatus status, Long customerId, String keyword, Pageable pageable);
}
