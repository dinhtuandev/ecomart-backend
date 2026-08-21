package com.ecomart.dto.response;

import com.ecomart.entity.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodStatsResponse {

    private PaymentMethod paymentMethod;
    private Long orderCount;
    private BigDecimal totalAmount;
    private Double percentage;
}
