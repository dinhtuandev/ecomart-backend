package com.ecomart.controller;

import com.ecomart.dto.request.CreateReviewRequest;
import com.ecomart.dto.request.UpdateReviewRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ReviewResponse;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewResponse response = reviewService.createReview(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá sản phẩm thành công.", response));
    }

    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        ReviewResponse response = reviewService.updateReview(currentUser.getId(), reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đánh giá thành công.", response));
    }

    @GetMapping("/me/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getCustomerReviews(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageResponse<ReviewResponse> response = reviewService.getCustomerReviews(currentUser.getId(), page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá của bạn thành công.", response));
    }
}
