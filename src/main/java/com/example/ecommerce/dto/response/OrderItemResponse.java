package com.example.ecommerce.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId,
                                String sku,
                                String productName,
                                BigDecimal unitPrice,
                                int quantity,
                                BigDecimal lineTotal) {
}
