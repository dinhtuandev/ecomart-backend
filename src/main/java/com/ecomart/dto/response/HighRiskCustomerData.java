package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighRiskCustomerData {

    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Long totalOrders;
    private Long cancelledOrders;
    private Double cancellationRate;
}
