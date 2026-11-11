package com.example.ecommerce.modules.order.entity;

/**
 * Order lifecycle:
 * <pre>
 * PENDING --pay--> PAID --ship--> SHIPPED --deliver--> DELIVERED
 *    |               |
 *    +----cancel-----+--> CANCELLED
 * </pre>
 * Cash-on-delivery orders may go from PENDING straight to SHIPPED and are marked paid on delivery.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
