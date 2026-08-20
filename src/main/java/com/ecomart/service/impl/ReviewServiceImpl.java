package com.ecomart.service.impl;

import com.ecomart.dto.request.CreateReviewRequest;
import com.ecomart.dto.request.UpdateReviewRequest;
import com.ecomart.dto.request.UpdateReviewVisibilityRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ProductReviewSummaryResponse;
import com.ecomart.dto.response.RatingBreakdownResponse;
import com.ecomart.dto.response.ReviewResponse;
import com.ecomart.entity.*;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.exception.ConflictException;
import com.ecomart.exception.ForbiddenException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnprocessableEntityException;
import com.ecomart.repository.OrderItemRepository;
import com.ecomart.repository.ProductRepository;
import com.ecomart.repository.ReviewRepository;
import com.ecomart.repository.UserRepository;
import com.ecomart.service.ReviewService;
import com.ecomart.specification.ReviewSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));

        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Món hàng không tồn tại với ID: " + request.getOrderItemId()));

        Order order = orderItem.getOrder();
        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền đánh giá món hàng không thuộc đơn hàng của mình");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new UnprocessableEntityException("Chỉ có thể đánh giá sản phẩm khi đơn hàng đã hoàn thành (COMPLETED)");
        }

        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new ConflictException("Món hàng trong đơn này đã được đánh giá trước đó");
        }

        Review review = Review.builder()
                .user(user)
                .product(orderItem.getProduct())
                .orderItem(orderItem)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVisible(true)
                .build();

        Review savedReview = reviewRepository.save(review);
        return mapToReviewResponse(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updatedReview = reviewRepository.save(review);
        return mapToReviewResponse(updatedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getCustomerReviews(Long userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByUserId(userId, pageable);

        List<ReviewResponse> items = reviewPage.getContent().stream()
                .map(this::mapToReviewResponse)
                .toList();

        return PageResponse.from(items, reviewPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewSummaryResponse getProductReviews(Long productId, Integer rating, int page, int pageSize) {
        // Validate product exists -> 404 if missing
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage;
        if (rating != null) {
            reviewPage = reviewRepository.findByProductIdAndRatingAndIsVisibleTrue(productId, rating, pageable);
        } else {
            reviewPage = reviewRepository.findByProductIdAndIsVisibleTrue(productId, pageable);
        }

        long totalCount = reviewRepository.countByProductIdAndIsVisibleTrue(productId);
        Double rawAvg = reviewRepository.getAverageRatingByProductId(productId);
        Double averageRating = (rawAvg != null) ? Math.round(rawAvg * 10.0) / 10.0 : 0.0;

        RatingBreakdownResponse breakdown = RatingBreakdownResponse.builder()
                .star5(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(productId, 5))
                .star4(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(productId, 4))
                .star3(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(productId, 3))
                .star2(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(productId, 2))
                .star1(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(productId, 1))
                .build();

        List<ReviewResponse> items = reviewPage.getContent().stream()
                .map(this::mapToReviewResponse)
                .toList();

        return ProductReviewSummaryResponse.builder()
                .averageRating(averageRating)
                .reviewCount(totalCount)
                .ratingBreakdown(breakdown)
                .reviews(PageResponse.from(items, reviewPage))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAdminReviews(
            int page,
            int pageSize,
            Long productId,
            Integer rating,
            Boolean isVisible,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Review> spec = ReviewSpecification.filterAdminReviews(productId, rating, isVisible, keyword, fromDate, toDate);

        Page<Review> reviewPage = reviewRepository.findAll(spec, pageable);
        List<ReviewResponse> items = reviewPage.getContent().stream()
                .map(this::mapToReviewResponse)
                .toList();

        return PageResponse.from(items, reviewPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getAdminReviewDetail(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));
        return mapToReviewResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReviewVisibility(Long reviewId, UpdateReviewVisibilityRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));

        review.setVisible(request.getIsVisible());
        Review updatedReview = reviewRepository.save(review);
        return mapToReviewResponse(updatedReview);
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        Product product = review.getProduct();
        String primaryImageUrl = null;
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            primaryImageUrl = product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElseGet(() -> product.getImages().get(0).getImageUrl());
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productImageUrl(primaryImageUrl)
                .orderItemId(review.getOrderItem() != null ? review.getOrderItem().getId() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userFullName(review.getUser() != null ? review.getUser().getFullName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .isVisible(review.isVisible())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
