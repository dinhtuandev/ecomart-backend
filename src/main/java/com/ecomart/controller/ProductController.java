package com.ecomart.controller;

import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ProductResponse;
import com.ecomart.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getPublicProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long certificationId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minEcoScore,
            @RequestParam(required = false) String sort
    ) {
        PageResponse<ProductResponse> response = productService.getPublicProducts(
                page, pageSize, keyword, categoryId, brandId, certificationId, minPrice, maxPrice, minEcoScore, sort
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công.", response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getPublicProductDetail(@PathVariable Long productId) {
        ProductResponse response = productService.getPublicProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công.", response));
    }
}
