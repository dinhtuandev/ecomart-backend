package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {

    private Long id;
    private String gateway;
    private BigDecimal amount;
    private String gatewayTransactionNo;
    private String status;
    private LocalDateTime createdAt;
}
