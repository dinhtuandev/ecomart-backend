package com.ecomart.controller;

import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ProductReviewSummaryResponse;
import com.ecomart.dto.response.RatingBreakdownResponse;
import com.ecomart.dto.response.ReviewResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/products/{id}/reviews - Xem công khai đánh giá sản phẩm trả về HTTP 200")
    void getProductReviews_Returns200() throws Exception {
        RatingBreakdownResponse breakdown = RatingBreakdownResponse.builder().star5(5).star4(0).star3(0).star2(0).star1(0).build();
        ProductReviewSummaryResponse summary = ProductReviewSummaryResponse.builder()
                .averageRating(5.0)
                .reviewCount(5L)
                .ratingBreakdown(breakdown)
                .reviews(PageResponse.from(List.of(), new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)))
                .build();

        when(reviewService.getProductReviews(100L, null, 1, 10)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/products/100/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.averageRating").value(5.0))
                .andExpect(jsonPath("$.data.reviewCount").value(5));
    }
}
