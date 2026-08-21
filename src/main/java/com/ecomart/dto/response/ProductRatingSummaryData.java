package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummaryData {

    private Long productId;
    private String productName;
    private Double averageRating;
    private Long totalReviews;
}
