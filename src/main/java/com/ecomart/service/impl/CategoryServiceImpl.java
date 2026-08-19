package com.ecomart.service.impl;

import com.ecomart.dto.request.CategoryRequest;
import com.ecomart.dto.response.CategoryResponse;
import com.ecomart.entity.Category;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.CategoryRepository;
import com.ecomart.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(this::mapToCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesForAdmin() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tên danh mục '" + request.getName() + "' đã tồn tại");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại với ID: " + categoryId));

        if (request.getName() != null && categoryRepository.existsByNameAndIdNot(request.getName(), categoryId)) {
            throw new BadRequestException("Tên danh mục '" + request.getName() + "' đã được sử dụng bởi danh mục khác");
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            category.setActive(request.getIsActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(updatedCategory);
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .isActive(category.isActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
