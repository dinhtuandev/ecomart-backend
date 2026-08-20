package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewSummaryResponse {

    private Double averageRating;
    private long reviewCount;
    private RatingBreakdownResponse ratingBreakdown;
    private PageResponse<ReviewResponse> reviews;
}
