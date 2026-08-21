package com.ecomart.controller;

import com.ecomart.dto.response.*;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.ReportGroupBy;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal adminPrincipal;
    private UsernamePasswordAuthenticationToken adminAuthToken;

    @BeforeEach
    void setUp() {
        adminPrincipal = UserPrincipal.create(99L, "admin@ecomart.com", "pass", "ADMIN", true);
        adminAuthToken = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(adminAuthToken);
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/dashboard-summary - Admin xem Dashboard thành công trả về HTTP 200")
    void getDashboardSummary_Returns200() throws Exception {
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .totalRevenue(BigDecimal.valueOf(10000000))
                .totalOrders(25L)
                .completedOrders(20L)
                .totalCustomers(100L)
                .totalProducts(50L)
                .lowStockProducts(4L)
                .newContactMessages(2L)
                .recentOrders(new ArrayList<>())
                .build();

        when(reportService.getDashboardSummary()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/reports/dashboard-summary")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").value(10000000))
                .andExpect(jsonPath("$.data.totalOrders").value(25))
                .andExpect(jsonPath("$.data.totalCustomers").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/revenue - Admin xem Báo cáo doanh thu trả về HTTP 200")
    void getRevenueReport_Returns200() throws Exception {
        RevenuePeriodData item = new RevenuePeriodData("2026-08-01", BigDecimal.valueOf(1000000), 5L);
        RevenueReportResponse response = RevenueReportResponse.builder()
                .totalRevenue(BigDecimal.valueOf(1000000))
                .totalCompletedOrders(5L)
                .averageOrderValue(BigDecimal.valueOf(200000))
                .groupBy(ReportGroupBy.DAY)
                .fromDate(LocalDate.of(2026, 8, 1))
                .toDate(LocalDate.of(2026, 8, 1))
                .items(List.of(item))
                .build();

        when(reportService.getRevenueReport(any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").value(1000000))
                .andExpect(jsonPath("$.data.items[0].period").value("2026-08-01"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/top-selling-products - Admin xem top sản phẩm bán chạy trả về HTTP 200")
    void getTopSellingProducts_Returns200() throws Exception {
        TopSellingProductResponse item = TopSellingProductResponse.builder()
                .productId(100L)
                .productName("Bình giữ nhiệt Eco")
                .totalQuantitySold(50L)
                .totalRevenue(BigDecimal.valueOf(10000000))
                .build();

        when(reportService.getTopSellingProducts(anyInt(), any(), any())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/admin/reports/top-selling-products")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productId").value(100L))
                .andExpect(jsonPath("$.data[0].totalQuantitySold").value(50L));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/sales-by-category - Trả về 200 kèm tỷ trọng doanh số danh mục")
    void getSalesByCategory_Returns200() throws Exception {
        CategorySalesResponse cat = CategorySalesResponse.builder()
                .categoryId(1L)
                .categoryName("Gia dụng")
                .quantitySold(20L)
                .revenue(BigDecimal.valueOf(5000000))
                .percentage(100.0)
                .build();

        when(reportService.getSalesByCategory(any(), any())).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/v1/admin/reports/sales-by-category")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoryName").value("Gia dụng"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/sales-by-brand - Trả về 200 kèm tỷ trọng doanh số thương hiệu")
    void getSalesByBrand_Returns200() throws Exception {
        BrandSalesResponse brand = BrandSalesResponse.builder()
                .brandId(1L)
                .brandName("EcoLife")
                .quantitySold(30L)
                .revenue(BigDecimal.valueOf(8000000))
                .percentage(100.0)
                .build();

        when(reportService.getSalesByBrand(any(), any())).thenReturn(List.of(brand));

        mockMvc.perform(get("/api/v1/admin/reports/sales-by-brand")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].brandName").value("EcoLife"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/payment-methods - Trả về 200 kèm cơ cấu phương thức thanh toán")
    void getPaymentMethodStats_Returns200() throws Exception {
        PaymentMethodStatsResponse stats = PaymentMethodStatsResponse.builder()
                .paymentMethod(PaymentMethod.COD)
                .orderCount(15L)
                .totalAmount(BigDecimal.valueOf(4500000))
                .percentage(100.0)
                .build();

        when(reportService.getPaymentMethodStats(any(), any())).thenReturn(List.of(stats));

        mockMvc.perform(get("/api/v1/admin/reports/payment-methods")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].paymentMethod").value("COD"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/customer-growth - Trả về 200 kèm biểu đồ tăng trưởng khách hàng")
    void getCustomerGrowth_Returns200() throws Exception {
        CustomerGrowthResponse growth = CustomerGrowthResponse.builder()
                .totalNewCustomers(10L)
                .groupBy(ReportGroupBy.DAY)
                .items(List.of(new CustomerGrowthData("2026-08-01", 10L)))
                .build();

        when(reportService.getCustomerGrowth(any(), any(), any())).thenReturn(growth);

        mockMvc.perform(get("/api/v1/admin/reports/customer-growth")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNewCustomers").value(10L));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/top-customers - Trả về 200 kèm danh sách VIP chi tiêu cao nhất")
    void getTopCustomers_Returns200() throws Exception {
        TopCustomerResponse vip = TopCustomerResponse.builder()
                .userId(1L)
                .fullName("Nguyen Van VIP")
                .totalSpent(BigDecimal.valueOf(20000000))
                .build();

        when(reportService.getTopCustomers(anyInt())).thenReturn(List.of(vip));

        mockMvc.perform(get("/api/v1/admin/reports/top-customers")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fullName").value("Nguyen Van VIP"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/customer-insights - Trả về 200 kèm phân tích sức khỏe khách hàng")
    void getCustomerInsights_Returns200() throws Exception {
        CustomerInsightsResponse insights = CustomerInsightsResponse.builder()
                .totalCustomers(100L)
                .activeCustomers(90L)
                .repeatPurchaseRate(45.0)
                .highRiskCustomers(new ArrayList<>())
                .build();

        when(reportService.getCustomerInsights()).thenReturn(insights);

        mockMvc.perform(get("/api/v1/admin/reports/customer-insights")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCustomers").value(100L))
                .andExpect(jsonPath("$.data.repeatPurchaseRate").value(45.0));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/inventory-alerts - Trả về 200 kèm danh sách cảnh báo tồn kho")
    void getInventoryAlerts_Returns200() throws Exception {
        InventoryAlertResponse alert = InventoryAlertResponse.builder()
                .productId(10L)
                .productName("Sản phẩm hết hàng")
                .currentStock(0)
                .isOutOfStock(true)
                .build();

        when(reportService.getInventoryAlerts(any())).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/v1/admin/reports/inventory-alerts")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productName").value("Sản phẩm hết hàng"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/review-insights - Trả về 200 kèm chất lượng đánh giá và cảnh báo 1-2 sao")
    void getReviewInsights_Returns200() throws Exception {
        ReviewInsightsResponse insights = ReviewInsightsResponse.builder()
                .totalReviews(50L)
                .satisfactionRate(92.0)
                .ratingDistribution(Map.of(5, 40L, 4, 6L, 3, 2L, 2, 1L, 1, 1L))
                .recentCriticalReviews(new ArrayList<>())
                .lowestRatedProducts(new ArrayList<>())
                .build();

        when(reportService.getReviewInsights()).thenReturn(insights);

        mockMvc.perform(get("/api/v1/admin/reports/review-insights")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.satisfactionRate").value(92.0));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/eco-impact - Trả về 200 kèm thống kê tác động xanh")
    void getEcoImpact_Returns200() throws Exception {
        EcoImpactResponse impact = EcoImpactResponse.builder()
                .averageEcoScore(4.5)
                .highEcoScoreProductsSold(120L)
                .build();

        when(reportService.getEcoImpact()).thenReturn(impact);

        mockMvc.perform(get("/api/v1/admin/reports/eco-impact")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.averageEcoScore").value(4.5));
    }
}
