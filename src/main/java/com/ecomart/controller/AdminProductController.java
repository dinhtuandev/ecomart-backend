package com.ecomart.controller;

import com.ecomart.dto.request.ProductImageRequest;
import com.ecomart.dto.request.ProductRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ProductResponse;
import com.ecomart.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAdminProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean isVisible
    ) {
        PageResponse<ProductResponse> response = productService.getAdminProducts(
                page, pageSize, keyword, categoryId, brandId, isVisible
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm quản trị thành công.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm mới thành công.", response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getAdminProductDetail(@PathVariable Long productId) {
        ProductResponse response = productService.getAdminProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công.", response));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable Long productId,
                                                                       @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin sản phẩm thành công.", response));
    }

    @PutMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductImages(@PathVariable Long productId,
                                                                             @Valid @RequestBody List<ProductImageRequest> imageRequests) {
        ProductResponse response = productService.updateProductImages(productId, imageRequests);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hình ảnh sản phẩm thành công.", response));
    }
}
