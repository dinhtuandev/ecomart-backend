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
public class InventoryAlertResponse {

    private Long productId;
    private String productName;
    private String categoryName;
    private Integer currentStock;
    private BigDecimal sellingPrice;
    private boolean isOutOfStock;
}
