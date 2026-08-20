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
public class VNPayReturnResponse {

    private String orderCode;
    private String transactionNo;
    private BigDecimal amount;
    private String bankCode;
    private String cardType;
    private String orderInfo;
    private String payDate;
    private String responseCode;
    private boolean isSuccess;
    private String message;
}
