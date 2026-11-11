package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CategoryRequest;
import com.example.ecommerce.dto.response.CategoryResponse;
import com.example.ecommerce.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    /** Customers and anonymous visitors only see active categories. */
    CategoryResponse getById(Long id);

    /**
     * @param active admin-only filter; non-admins always get active categories
     */
    PageResponse<CategoryResponse> search(String keyword, Boolean active, Pageable pageable);
}
