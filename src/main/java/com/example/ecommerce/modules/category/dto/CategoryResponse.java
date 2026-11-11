package com.example.ecommerce.modules.category.dto;

import java.time.LocalDateTime;

public record CategoryResponse(Long id,
                               String name,
                               String slug,
                               String description,
                               boolean active,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
}
