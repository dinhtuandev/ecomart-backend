package com.ecomart.controller;

import com.ecomart.dto.request.CategoryRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.CategoryResponse;
import com.ecomart.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategoriesForAdmin() {
        List<CategoryResponse> response = categoryService.getAllCategoriesForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Lấy tất cả danh mục sản phẩm thành công.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo danh mục sản phẩm mới thành công.", response));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable Long categoryId,
                                                                         @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục sản phẩm thành công.", response));
    }
}
