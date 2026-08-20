package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Long orderItemId;
    private Long userId;
    private String userFullName;
    private Integer rating;
    private String comment;
    private boolean isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
