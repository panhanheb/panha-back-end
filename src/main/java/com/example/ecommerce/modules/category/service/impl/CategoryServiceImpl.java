package com.example.ecommerce.modules.category.service.impl;

import lombok.RequiredArgsConstructor;
import com.example.ecommerce.modules.category.dto.CategoryRequest;
import com.example.ecommerce.modules.category.dto.CategoryResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.modules.category.entity.Category;
import com.example.ecommerce.modules.audit.service.AuditAction;
import com.example.ecommerce.common.security.RoleName;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ConflictException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.modules.category.mapper.CategoryMapper;
import com.example.ecommerce.modules.category.repository.CategoryRepository;
import com.example.ecommerce.modules.product.repository.ProductRepository;
import com.example.ecommerce.common.security.SecurityUtils;
import com.example.ecommerce.modules.audit.service.AuditService;
import com.example.ecommerce.modules.category.service.CategoryService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String ENTITY = "Category";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        apply(category, request, null);
        categoryRepository.save(category);
        auditService.log(AuditAction.CREATE, ENTITY, category.getId(), "Created category '" + category.getName() + "'");
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        apply(category, request, id);
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Updated category '" + category.getName() + "'");
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category '" + category.getName()
                    + "' still contains products; move or delete them first, or deactivate the category");
        }
        categoryRepository.delete(category);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted category '" + category.getName() + "'");
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = findCategory(id);
        if (!category.isActive() && !isAdmin()) {
            throw new ResourceNotFoundException(ENTITY, id);
        }
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> search(String keyword, Boolean active, Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Boolean activeFilter = isAdmin() ? active : Boolean.TRUE;
        return PageResponse.from(categoryRepository.search(kw, activeFilter, pageable), categoryMapper::toResponse);
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Category category, CategoryRequest request, Long id) {
        String name = request.name().trim();
        boolean nameTaken = id == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (nameTaken) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "Category '" + name + "' already exists");
        }
        String slug = StringUtils.hasText(request.slug()) ? request.slug().trim() : slugify(name);
        if (slug.isEmpty()) {
            throw new BadRequestException("Could not derive a slug from the name; please provide one");
        }
        boolean slugTaken = id == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, id);
        if (slugTaken) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "Category slug '" + slug + "' already exists");
        }
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        if (request.active() != null) {
            category.setActive(request.active());
        }
    }

    /** "Phones & Tablets" -> "phones-tablets". */
    static String slugify(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private static boolean isAdmin() {
        return SecurityUtils.hasRole(RoleName.ROLE_ADMIN);
    }
}
