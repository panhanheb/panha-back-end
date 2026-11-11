package com.example.ecommerce.modules.cart.dto;

import java.math.BigDecimal;

/**
 * @param unitPrice current catalog price
 * @param available false when the product was deactivated or there is not enough stock for this quantity;
 *                  checkout is refused until the item is fixed or removed
 */
public record CartItemResponse(Long productId,
                               String sku,
                               String productName,
                               String imageUrl,
                               BigDecimal unitPrice,
                               int quantity,
                               BigDecimal lineTotal,
                               int stockQuantity,
                               boolean available) {
}
