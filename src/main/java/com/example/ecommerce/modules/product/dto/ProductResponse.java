package com.example.ecommerce.modules.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(Long id,
                              String sku,
                              String name,
                              String description,
                              BigDecimal price,
                              int stockQuantity,
                              boolean inStock,
                              String imageUrl,
                              boolean active,
                              Long categoryId,
                              String categoryName,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
}
