package com.ecomart.service;

import com.ecomart.dto.response.*;
import com.ecomart.entity.enums.ReportGroupBy;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    DashboardSummaryResponse getDashboardSummary();

    RevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate, ReportGroupBy groupBy);

    List<TopSellingProductResponse> getTopSellingProducts(int limit, LocalDate fromDate, LocalDate toDate);

    List<CategorySalesResponse> getSalesByCategory(LocalDate fromDate, LocalDate toDate);

    List<BrandSalesResponse> getSalesByBrand(LocalDate fromDate, LocalDate toDate);

    List<PaymentMethodStatsResponse> getPaymentMethodStats(LocalDate fromDate, LocalDate toDate);

    CustomerGrowthResponse getCustomerGrowth(LocalDate fromDate, LocalDate toDate, ReportGroupBy groupBy);

    List<TopCustomerResponse> getTopCustomers(int limit);

    CustomerInsightsResponse getCustomerInsights();

    List<InventoryAlertResponse> getInventoryAlerts(Integer threshold);

    ReviewInsightsResponse getReviewInsights();

    EcoImpactResponse getEcoImpact();
}
