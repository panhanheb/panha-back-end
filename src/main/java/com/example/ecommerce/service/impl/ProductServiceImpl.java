package com.example.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.dto.request.StockUpdateRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.model.Category;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.enums.AuditAction;
import com.example.ecommerce.model.enums.RoleName;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ErrorCode;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ProductSpecifications;
import com.example.ecommerce.security.SecurityUtils;
import com.example.ecommerce.service.AuditService;
import com.example.ecommerce.service.PricingService;
import com.example.ecommerce.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String ENTITY = "Product";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductMapper productMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        validateSku(request.sku(), null);
        Product product = new Product();
        apply(product, request);
        productRepository.save(product);
        auditService.log(AuditAction.CREATE, ENTITY, product.getId(), "Created product " + product.getSku());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        validateSku(request.sku(), id);
        apply(product, request);
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Updated product " + product.getSku());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long id, StockUpdateRequest request) {
        // Lock the row so a concurrent checkout cannot deduct stock between our read and write.
        Product product = productRepository.findAllByIdForUpdate(List.of(id)).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
        int previous = product.getStockQuantity();
        product.setStockQuantity(request.stockQuantity());
        auditService.log(AuditAction.UPDATE, ENTITY, id,
                "Stock of " + product.getSku() + " changed from " + previous + " to " + request.stockQuantity());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        if (orderItemRepository.existsByProductId(id)) {
            throw new ConflictException("Product " + product.getSku()
                    + " appears in existing orders and cannot be deleted; deactivate it instead");
        }
        cartItemRepository.deleteByProductId(id);
        productRepository.delete(product);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted product " + product.getSku());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = findProduct(id);
        if (!product.isPurchasable() && !isAdmin()) {
            throw new ResourceNotFoundException(ENTITY, id);
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Long categoryId, BigDecimal minPrice,
                                                BigDecimal maxPrice, Boolean inStock, Boolean active,
                                                Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must not be greater than maxPrice");
        }
        boolean admin = isAdmin();
        return PageResponse.from(
                productRepository.findAll(ProductSpecifications.filter(
                        keyword, categoryId, minPrice, maxPrice, inStock, active, !admin), pageable),
                productMapper::toResponse);
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        product.setSku(request.sku().trim().toUpperCase());
        product.setName(request.name().trim());
        product.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        product.setPrice(PricingService.money(request.price()));
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(StringUtils.hasText(request.imageUrl()) ? request.imageUrl().trim() : null);
        product.setCategory(category);
        if (request.active() != null) {
            product.setActive(request.active());
        }
    }

    private void validateSku(String sku, Long id) {
        String normalized = sku.trim();
        boolean taken = id == null
                ? productRepository.existsBySkuIgnoreCase(normalized)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(normalized, id);
        if (taken) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "SKU '" + normalized.toUpperCase() + "' already exists");
        }
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private static boolean isAdmin() {
        return SecurityUtils.hasRole(RoleName.ROLE_ADMIN);
    }
}
