package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcoImpactResponse {

    private Double averageEcoScore;
    private Long certifiedProductsSold;
    private Long highEcoScoreProductsSold;
}
