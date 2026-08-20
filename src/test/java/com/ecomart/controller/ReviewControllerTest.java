package com.ecomart.controller;

import com.ecomart.dto.request.CreateReviewRequest;
import com.ecomart.dto.request.UpdateReviewRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ReviewResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal userPrincipal;
    private UsernamePasswordAuthenticationToken authToken;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.create(1L, "user@example.com", "pass", "CUSTOMER", true);
        authToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        reviewResponse = ReviewResponse.builder()
                .id(10L)
                .productId(100L)
                .productName("Bình giữ nhiệt Eco")
                .orderItemId(5001L)
                .userId(1L)
                .userFullName("Nguyen Van A")
                .rating(5)
                .comment("Rất tốt")
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/reviews - Tạo đánh giá thành công trả về HTTP 201")
    void createReview_Returns201() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderItemId(5001L)
                .rating(5)
                .comment("Rất tốt")
                .build();

        when(reviewService.createReview(eq(1L), any(CreateReviewRequest.class))).thenReturn(reviewResponse);

        mockMvc.perform(post("/api/v1/reviews")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    @DisplayName("PATCH /api/v1/reviews/{id} - Chỉnh sửa đánh giá trả về HTTP 200")
    void updateReview_Returns200() throws Exception {
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(4).comment("Sửa lại 4 sao").build();
        reviewResponse.setRating(4);

        when(reviewService.updateReview(eq(1L), eq(10L), any(UpdateReviewRequest.class))).thenReturn(reviewResponse);

        mockMvc.perform(patch("/api/v1/reviews/10")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(4));
    }

    @Test
    @DisplayName("GET /api/v1/me/reviews - Khách hàng xem đánh giá của mình trả về HTTP 200")
    void getCustomerReviews_Returns200() throws Exception {
        PageResponse<ReviewResponse> pageResponse = PageResponse.from(List.of(reviewResponse), new PageImpl<>(List.of(reviewResponse), PageRequest.of(0, 10), 1));

        when(reviewService.getCustomerReviews(1L, 1, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/me/reviews")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(10L));
    }
}
