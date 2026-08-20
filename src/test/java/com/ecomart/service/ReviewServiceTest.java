package com.ecomart.service;

import com.ecomart.dto.request.CreateReviewRequest;
import com.ecomart.dto.request.UpdateReviewRequest;
import com.ecomart.dto.request.UpdateReviewVisibilityRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ProductReviewSummaryResponse;
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
import com.ecomart.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User user;
    private User otherUser;
    private Product product;
    private Order order;
    private OrderItem orderItem;
    private Review review;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@example.com").fullName("Nguyen Van A").build();
        otherUser = User.builder().id(2L).email("other@example.com").fullName("Tran Van B").build();

        product = Product.builder()
                .id(100L)
                .name("Bình giữ nhiệt Eco")
                .sellingPrice(BigDecimal.valueOf(200000))
                .build();

        order = Order.builder()
                .id(1000L)
                .user(user)
                .orderCode("EM-20260820-0001")
                .status(OrderStatus.COMPLETED)
                .build();

        orderItem = OrderItem.builder()
                .id(5001L)
                .order(order)
                .product(product)
                .productName("Bình giữ nhiệt Eco")
                .unitPrice(BigDecimal.valueOf(200000))
                .quantity(1)
                .lineTotal(BigDecimal.valueOf(200000))
                .build();

        review = Review.builder()
                .id(10L)
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .rating(5)
                .comment("Sản phẩm rất tốt và thân thiện môi trường")
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Tạo đánh giá thành công khi đơn hàng COMPLETED và chưa từng đánh giá")
    void createReview_Success() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderItemId(5001L)
                .rating(5)
                .comment("Rất hài lòng")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderItemRepository.findById(5001L)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(5001L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewResponse response = reviewService.createReview(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getProductId()).isEqualTo(100L);
        assertThat(response.getOrderItemId()).isEqualTo(5001L);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Ném 404 khi orderItemId không tồn tại")
    void createReview_Throws404_WhenOrderItemNotFound() {
        CreateReviewRequest request = CreateReviewRequest.builder().orderItemId(9999L).rating(5).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderItemRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Món hàng không tồn tại");
    }

    @Test
    @DisplayName("Ném 403 khi đánh giá món hàng không thuộc quyền sở hữu của mình")
    void createReview_Throws403_WhenOrderItemNotOwnedByUser() {
        CreateReviewRequest request = CreateReviewRequest.builder().orderItemId(5001L).rating(5).build();
        order.setUser(otherUser); // Thuộc sở hữu của user khác

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderItemRepository.findById(5001L)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("không thuộc đơn hàng của mình");
    }

    @Test
    @DisplayName("Ném 422 khi đánh giá món hàng thuộc đơn chưa hoàn thành (PENDING/CONFIRMED)")
    void createReview_Throws422_WhenOrderNotCompleted() {
        CreateReviewRequest request = CreateReviewRequest.builder().orderItemId(5001L).rating(5).build();
        order.setStatus(OrderStatus.CONFIRMED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderItemRepository.findById(5001L)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("đơn hàng đã hoàn thành (COMPLETED)");
    }

    @Test
    @DisplayName("Ném 409 khi món hàng trong đơn đã được đánh giá trước đó")
    void createReview_Throws409_WhenAlreadyReviewed() {
        CreateReviewRequest request = CreateReviewRequest.builder().orderItemId(5001L).rating(5).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderItemRepository.findById(5001L)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(5001L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã được đánh giá trước đó");
    }

    @Test
    @DisplayName("Khách hàng chỉnh sửa đánh giá của mình thành công")
    void updateReview_Success() {
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(4).comment("Cập nhật lại 4 sao").build();

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewResponse response = reviewService.updateReview(1L, 10L, request);

        assertThat(response).isNotNull();
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getComment()).isEqualTo("Cập nhật lại 4 sao");
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("Ném 403 khi người dùng khác cố tình chỉnh sửa đánh giá không phải của mình")
    void updateReview_Throws403_WhenUserIsNotAuthor() {
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(4).comment("Hack review").build();

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(2L, 10L, request)) // userId = 2L != author (1L)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("không có quyền chỉnh sửa");
    }

    @Test
    @DisplayName("Ném 404 khi reviewId cần cập nhật không tồn tại")
    void updateReview_Throws404_WhenReviewNotFound() {
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(4).build();
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(1L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Khách hàng lấy danh sách đánh giá của mình thành công")
    void getCustomerReviews_Success() {
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        PageResponse<ReviewResponse> response = reviewService.getCustomerReviews(1L, 1, 10);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Khách vãng lai xem đánh giá sản phẩm: tính toán đúng trung bình sao và rating breakdown")
    void getProductReviews_Success_ComputesAverageAndBreakdown() {
        when(productRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.findByProductIdAndIsVisibleTrue(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(reviewRepository.countByProductIdAndIsVisibleTrue(100L)).thenReturn(10L);
        when(reviewRepository.getAverageRatingByProductId(100L)).thenReturn(4.66);
        when(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(100L, 5)).thenReturn(7L);
        when(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(100L, 4)).thenReturn(2L);
        when(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(100L, 3)).thenReturn(1L);
        when(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(100L, 2)).thenReturn(0L);
        when(reviewRepository.countByProductIdAndRatingAndIsVisibleTrue(100L, 1)).thenReturn(0L);

        ProductReviewSummaryResponse response = reviewService.getProductReviews(100L, null, 1, 10);

        assertThat(response).isNotNull();
        assertThat(response.getAverageRating()).isEqualTo(4.7); // 4.66 làm tròn 4.7
        assertThat(response.getReviewCount()).isEqualTo(10L);
        assertThat(response.getRatingBreakdown().getStar5()).isEqualTo(7L);
        assertThat(response.getRatingBreakdown().getStar4()).isEqualTo(2L);
        assertThat(response.getReviews().getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Xem đánh giá sản phẩm chưa có đánh giá nào -> trả về averageRating = 0.0")
    void getProductReviews_Returns0Average_WhenNoReviews() {
        when(productRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.findByProductIdAndIsVisibleTrue(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(reviewRepository.countByProductIdAndIsVisibleTrue(100L)).thenReturn(0L);
        when(reviewRepository.getAverageRatingByProductId(100L)).thenReturn(null);

        ProductReviewSummaryResponse response = reviewService.getProductReviews(100L, null, 1, 10);

        assertThat(response.getAverageRating()).isEqualTo(0.0);
        assertThat(response.getReviewCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Ném 404 khi xem đánh giá của sản phẩm không tồn tại")
    void getProductReviews_Throws404_WhenProductNotFound() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getProductReviews(999L, null, 1, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tồn tại");
    }

    @Test
    @DisplayName("Admin lấy danh sách đánh giá quản trị thành công")
    void getAdminReviews_Success() {
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        PageResponse<ReviewResponse> response = reviewService.getAdminReviews(1, 10, null, null, null, null, null, null);

        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Admin xem chi tiết đánh giá thành công")
    void getAdminReviewDetail_Success() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getAdminReviewDetail(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái hiển thị (ẩn/hiện) đánh giá thành công")
    void updateReviewVisibility_Success() {
        UpdateReviewVisibilityRequest request = UpdateReviewVisibilityRequest.builder().isVisible(false).build();

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewResponse response = reviewService.updateReviewVisibility(10L, request);

        assertThat(response).isNotNull();
        assertThat(review.isVisible()).isFalse();
        verify(reviewRepository).save(review);
    }
}
