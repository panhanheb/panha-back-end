package com.example.ecommerce.modules.product.service;

import com.example.ecommerce.modules.product.dto.ProductRequest;
import com.example.ecommerce.modules.product.dto.ProductResponse;
import com.example.ecommerce.modules.product.dto.StockUpdateRequest;
import com.example.ecommerce.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    ProductResponse updateStock(Long id, StockUpdateRequest request);

    /** Deletes a product that was never ordered; ordered products must be deactivated instead. */
    void delete(Long id);

    /** Customers and anonymous visitors only see purchasable products. */
    ProductResponse getById(Long id);

    /**
     * @param active admin-only filter; non-admins always get purchasable products
     */
    PageResponse<ProductResponse> search(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                         Boolean inStock, Boolean active, Pageable pageable);
}
