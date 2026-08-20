package com.ecomart.controller;

import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.ProductReviewSummaryResponse;
import com.ecomart.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductReviewSummaryResponse>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        ProductReviewSummaryResponse response = reviewService.getProductReviews(productId, rating, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá sản phẩm thành công.", response));
    }
}
