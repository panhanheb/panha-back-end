package com.example.ecommerce.modules.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId,
                                String sku,
                                String productName,
                                BigDecimal unitPrice,
                                int quantity,
                                BigDecimal lineTotal) {
}
