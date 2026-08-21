package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewInsightsResponse {

    private Long totalReviews;
    private Long visibleReviews;
    private Long hiddenReviews;
    private Double averagePlatformRating;
    private Double satisfactionRate;
    private Map<Integer, Long> ratingDistribution;
    private List<CriticalReviewAlertData> recentCriticalReviews;
    private List<ProductRatingSummaryData> lowestRatedProducts;
}
