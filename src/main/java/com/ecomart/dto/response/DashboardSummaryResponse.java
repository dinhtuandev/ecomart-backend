package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private BigDecimal totalRevenue;
    private long totalOrders;
    private long completedOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long cancelledOrders;
    private long totalCustomers;
    private long totalProducts;
    private long lowStockProducts;
    private long newContactMessages;
    private List<OrderResponse> recentOrders;
}
