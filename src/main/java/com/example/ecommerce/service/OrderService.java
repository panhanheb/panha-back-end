package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.request.CheckoutRequest;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.OrderSummaryResponse;
import com.example.ecommerce.dto.request.PaymentRequest;
import com.example.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.example.ecommerce.model.enums.OrderStatus;
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
