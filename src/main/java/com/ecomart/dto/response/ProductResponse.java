package com.ecomart.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal sellingPrice;
    private BigDecimal originalPrice;
    private Integer ecoScore;
    private String materialInfo;

    @JsonProperty("isVisible")
    private boolean isVisible;

    private CategoryResponse category;
    private BrandResponse brand;
    private List<CertificationResponse> certifications;
    private List<ProductImageResponse> images;
    private Integer quantityInStock;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
