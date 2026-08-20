package com.ecomart.service;

import com.ecomart.dto.request.CreateReviewRequest;
import com.ecomart.dto.request.UpdateReviewRequest;
import com.ecomart.dto.request.UpdateReviewVisibilityRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ProductReviewSummaryResponse;
import com.ecomart.dto.response.ReviewResponse;

import java.time.LocalDate;

public interface ReviewService {

    ReviewResponse createReview(Long userId, CreateReviewRequest request);

    ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request);

    PageResponse<ReviewResponse> getCustomerReviews(Long userId, int page, int pageSize);

    ProductReviewSummaryResponse getProductReviews(Long productId, Integer rating, int page, int pageSize);

    PageResponse<ReviewResponse> getAdminReviews(
            int page,
            int pageSize,
            Long productId,
            Integer rating,
            Boolean isVisible,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate
    );

    ReviewResponse getAdminReviewDetail(Long reviewId);

    ReviewResponse updateReviewVisibility(Long reviewId, UpdateReviewVisibilityRequest request);
}
