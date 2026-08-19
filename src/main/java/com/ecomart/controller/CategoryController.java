package com.ecomart.controller;

import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.CategoryResponse;
import com.ecomart.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        List<CategoryResponse> response = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục sản phẩm thành công.", response));
    }
}
