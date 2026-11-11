package com.example.ecommerce.modules.category.service;

import com.example.ecommerce.modules.category.dto.CategoryRequest;
import com.example.ecommerce.modules.category.dto.CategoryResponse;
import com.example.ecommerce.common.response.PageResponse;
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
