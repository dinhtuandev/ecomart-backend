package com.ecomart.service;

import com.ecomart.dto.request.CategoryRequest;
import com.ecomart.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getActiveCategories();

    List<CategoryResponse> getAllCategoriesForAdmin();

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long categoryId, CategoryRequest request);
}
