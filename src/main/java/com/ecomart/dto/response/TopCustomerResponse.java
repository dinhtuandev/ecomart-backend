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
public class TopCustomerResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Long completedOrdersCount;
    private BigDecimal totalSpent;
}
