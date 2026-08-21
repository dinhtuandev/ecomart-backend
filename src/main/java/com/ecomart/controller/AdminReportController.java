package com.ecomart.controller;

import com.ecomart.dto.response.*;
import com.ecomart.entity.enums.ReportGroupBy;
import com.ecomart.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary() {
        DashboardSummaryResponse response = reportService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tổng quan dashboard thành công.", response));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ReportGroupBy groupBy
    ) {
        RevenueReportResponse response = reportService.getRevenueReport(fromDate, toDate, groupBy);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo doanh thu thành công.", response));
    }

    @GetMapping("/top-selling-products")
    public ResponseEntity<ApiResponse<List<TopSellingProductResponse>>> getTopSellingProducts(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<TopSellingProductResponse> response = reportService.getTopSellingProducts(limit, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm bán chạy nhất thành công.", response));
    }

    @GetMapping("/sales-by-category")
    public ResponseEntity<ApiResponse<List<CategorySalesResponse>>> getSalesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<CategorySalesResponse> response = reportService.getSalesByCategory(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy tỷ trọng doanh số theo danh mục thành công.", response));
    }

    @GetMapping("/sales-by-brand")
    public ResponseEntity<ApiResponse<List<BrandSalesResponse>>> getSalesByBrand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<BrandSalesResponse> response = reportService.getSalesByBrand(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy tỷ trọng doanh số theo thương hiệu thành công.", response));
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentMethodStatsResponse>>> getPaymentMethodStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<PaymentMethodStatsResponse> response = reportService.getPaymentMethodStats(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy cơ cấu phương thức thanh toán thành công.", response));
    }

    @GetMapping("/customer-growth")
    public ResponseEntity<ApiResponse<CustomerGrowthResponse>> getCustomerGrowth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ReportGroupBy groupBy
    ) {
        CustomerGrowthResponse response = reportService.getCustomerGrowth(fromDate, toDate, groupBy);
        return ResponseEntity.ok(ApiResponse.success("Lấy biểu đồ tăng trưởng khách hàng thành công.", response));
    }

    @GetMapping("/top-customers")
    public ResponseEntity<ApiResponse<List<TopCustomerResponse>>> getTopCustomers(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<TopCustomerResponse> response = reportService.getTopCustomers(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khách hàng VIP chi tiêu cao nhất thành công.", response));
    }

    @GetMapping("/customer-insights")
    public ResponseEntity<ApiResponse<CustomerInsightsResponse>> getCustomerInsights() {
        CustomerInsightsResponse response = reportService.getCustomerInsights();
        return ResponseEntity.ok(ApiResponse.success("Lấy phân tích sức khỏe khách hàng và rủi ro thành công.", response));
    }

    @GetMapping("/inventory-alerts")
    public ResponseEntity<ApiResponse<List<InventoryAlertResponse>>> getInventoryAlerts(
            @RequestParam(defaultValue = "5") Integer threshold
    ) {
        List<InventoryAlertResponse> response = reportService.getInventoryAlerts(threshold);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cảnh báo tồn kho thành công.", response));
    }

    @GetMapping("/review-insights")
    public ResponseEntity<ApiResponse<ReviewInsightsResponse>> getReviewInsights() {
        ReviewInsightsResponse response = reportService.getReviewInsights();
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo chất lượng đánh giá và mức độ hài lòng thành công.", response));
    }

    @GetMapping("/eco-impact")
    public ResponseEntity<ApiResponse<EcoImpactResponse>> getEcoImpact() {
        EcoImpactResponse response = reportService.getEcoImpact();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê tác động sinh thái thành công.", response));
    }
}
