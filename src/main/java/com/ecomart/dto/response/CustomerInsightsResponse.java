package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInsightsResponse {

    private Long totalCustomers;
    private Long activeCustomers;
    private Long inactiveCustomers;
    private Long payingCustomers;
    private Long repeatCustomers;
    private Double repeatPurchaseRate;
    private List<HighRiskCustomerData> highRiskCustomers;
}
