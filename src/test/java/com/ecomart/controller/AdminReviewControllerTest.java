package com.ecomart.controller;

import com.ecomart.dto.request.UpdateReviewVisibilityRequest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReviewControllerTest {

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

    private UserPrincipal adminPrincipal;
    private UsernamePasswordAuthenticationToken authToken;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        adminPrincipal = UserPrincipal.create(99L, "admin@ecomart.com", "pass", "ADMIN", true);
        authToken = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        reviewResponse = ReviewResponse.builder()
                .id(10L)
                .productId(100L)
                .productName("Bình giữ nhiệt Eco")
                .userId(1L)
                .userFullName("Nguyen Van A")
                .rating(5)
                .comment("Đánh giá tốt")
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/reviews - Admin xem danh sách đánh giá trả về HTTP 200")
    void getAdminReviews_Returns200() throws Exception {
        PageResponse<ReviewResponse> pageResponse = PageResponse.from(List.of(reviewResponse), new PageImpl<>(List.of(reviewResponse), PageRequest.of(0, 10), 1));

        when(reviewService.getAdminReviews(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/reviews")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(10L));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/reviews/{id}/visibility - Admin ẩn/hiện đánh giá trả về HTTP 200")
    void updateReviewVisibility_Returns200() throws Exception {
        UpdateReviewVisibilityRequest request = UpdateReviewVisibilityRequest.builder().isVisible(false).build();
        reviewResponse.setVisible(false);

        when(reviewService.updateReviewVisibility(eq(10L), any(UpdateReviewVisibilityRequest.class)))
                .thenReturn(reviewResponse);

        mockMvc.perform(patch("/api/v1/admin/reviews/10/visibility")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visible").value(false));
    }
}
