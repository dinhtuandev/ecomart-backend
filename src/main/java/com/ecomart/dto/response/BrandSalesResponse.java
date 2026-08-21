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
public class BrandSalesResponse {

    private Long brandId;
    private String brandName;
    private Long quantitySold;
    private BigDecimal revenue;
    private Double percentage;
}
