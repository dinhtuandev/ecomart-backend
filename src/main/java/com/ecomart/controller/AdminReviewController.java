package com.ecomart.controller;

import com.ecomart.dto.request.UpdateReviewVisibilityRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ReviewResponse;
import com.ecomart.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAdminReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean isVisible,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        PageResponse<ReviewResponse> response = reviewService.getAdminReviews(
                page, pageSize, productId, rating, isVisible, keyword, fromDate, toDate
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá quản trị thành công.", response));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getAdminReviewDetail(@PathVariable Long reviewId) {
        ReviewResponse response = reviewService.getAdminReviewDetail(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đánh giá thành công.", response));
    }

    @PatchMapping("/{reviewId}/visibility")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReviewVisibility(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewVisibilityRequest request
    ) {
        ReviewResponse response = reviewService.updateReviewVisibility(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái hiển thị đánh giá thành công.", response));
    }
}
