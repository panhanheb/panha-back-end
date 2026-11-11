package com.example.ecommerce.modules.product.mapper;

import com.example.ecommerce.modules.category.entity.Category;
import com.example.ecommerce.modules.product.dto.ProductResponse;
import com.example.ecommerce.modules.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStockQuantity() > 0,
                product.getImageUrl(),
                product.isActive(),
                category.getId(),
                category.getName(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
