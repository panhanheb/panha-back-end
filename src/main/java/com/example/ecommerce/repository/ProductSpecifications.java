package com.example.ecommerce.repository;

import jakarta.persistence.criteria.Predicate;
import com.example.ecommerce.model.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Dynamic product search filters. Every argument is optional. */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /**
     * @param purchasableOnly when true, only active products in active categories (the customer view)
     * @param active          admin filter on the product's own active flag; ignored when purchasableOnly is true
     */
    public static Specification<Product> filter(String keyword, Long categoryId, BigDecimal minPrice,
                                                BigDecimal maxPrice, Boolean inStock, Boolean active,
                                                boolean purchasableOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (Boolean.TRUE.equals(inStock)) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            } else if (Boolean.FALSE.equals(inStock)) {
                predicates.add(cb.equal(root.get("stockQuantity"), 0));
            }
            if (purchasableOnly) {
                predicates.add(cb.isTrue(root.get("active")));
                predicates.add(cb.isTrue(root.get("category").get("active")));
            } else if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
