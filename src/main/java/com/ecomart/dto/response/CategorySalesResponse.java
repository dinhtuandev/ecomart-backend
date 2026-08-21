package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySalesResponse {

    private Long categoryId;
    private String categoryName;
    private Long quantitySold;
    private BigDecimal revenue;
    private Double percentage;
}
