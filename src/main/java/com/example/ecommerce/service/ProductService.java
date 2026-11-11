package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.dto.request.StockUpdateRequest;
import com.example.ecommerce.dto.response.PageResponse;
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
