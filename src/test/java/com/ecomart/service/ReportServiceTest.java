package com.ecomart.service;

import com.ecomart.dto.response.*;
import com.ecomart.entity.Order;
import com.ecomart.entity.Product;
import com.ecomart.entity.Review;
import com.ecomart.entity.User;
import com.ecomart.entity.enums.ContactStatus;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;
import com.ecomart.entity.enums.ReportGroupBy;
import com.ecomart.exception.BadRequestException;
import com.ecomart.repository.*;
import com.ecomart.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ContactMessageRepository contactMessageRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .id(1L)
                .orderCode("EM-20260821-0001")
                .status(OrderStatus.COMPLETED)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PAID)
                .totalAmount(BigDecimal.valueOf(500000))
                .orderedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .paymentTransactions(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Lấy tổng quan Dashboard thành công tổng hợp đầy đủ các metrics")
    void getDashboardSummary_Success() {
        when(orderRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(5000000));
        when(orderRepository.count()).thenReturn(20L);
        when(orderRepository.countByStatus(OrderStatus.COMPLETED)).thenReturn(15L);
        when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(2L);
        when(orderRepository.countByStatus(OrderStatus.CONFIRMED)).thenReturn(1L);
        when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(2L);

        when(userRepository.countByRoleName("CUSTOMER")).thenReturn(50L);
        when(productRepository.count()).thenReturn(30L);
        when(inventoryRepository.countByQuantityLessThanEqual(5)).thenReturn(3L);
        when(contactMessageRepository.countByStatus(ContactStatus.NEW)).thenReturn(4L);
        when(orderRepository.findTop5ByOrderByOrderedAtDesc()).thenReturn(List.of(sampleOrder));

        DashboardSummaryResponse response = reportService.getDashboardSummary();

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("5000000");
        assertThat(response.getTotalOrders()).isEqualTo(20L);
        assertThat(response.getCompletedOrders()).isEqualTo(15L);
        assertThat(response.getTotalCustomers()).isEqualTo(50L);
        assertThat(response.getLowStockProducts()).isEqualTo(3L);
        assertThat(response.getNewContactMessages()).isEqualTo(4L);
        assertThat(response.getRecentOrders()).hasSize(1);
    }

    @Test
    @DisplayName("Dashboard summary trả về totalRevenue = 0 khi hệ thống chưa có đơn COMPLETED nào")
    void getDashboardSummary_Returns0Revenue_WhenNoCompletedOrders() {
        when(orderRepository.calculateTotalRevenue()).thenReturn(null);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.findTop5ByOrderByOrderedAtDesc()).thenReturn(List.of());

        DashboardSummaryResponse response = reportService.getDashboardSummary();

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Báo cáo doanh thu gom nhóm theo Ngày (DAY) thành công")
    void getRevenueReport_Success_GroupedByDay() {
        LocalDate fromDate = LocalDate.of(2026, 8, 1);
        LocalDate toDate = LocalDate.of(2026, 8, 2);

        Object[] row1 = new Object[]{2026, 8, 1, BigDecimal.valueOf(300000), 2L};
        Object[] row2 = new Object[]{2026, 8, 2, BigDecimal.valueOf(700000), 3L};

        when(orderRepository.getRevenueGroupedByDay(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row1, row2));

        RevenueReportResponse response = reportService.getRevenueReport(fromDate, toDate, ReportGroupBy.DAY);

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("1000000");
        assertThat(response.getTotalCompletedOrders()).isEqualTo(5L);
        assertThat(response.getAverageOrderValue()).isEqualByComparingTo("200000.00");
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getPeriod()).isEqualTo("2026-08-01");
        assertThat(response.getItems().get(1).getPeriod()).isEqualTo("2026-08-02");
    }

    @Test
    @DisplayName("Báo cáo doanh thu gom nhóm theo Tháng (MONTH) thành công")
    void getRevenueReport_Success_GroupedByMonth() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 8, 31);

        Object[] row1 = new Object[]{2026, 7, BigDecimal.valueOf(5000000), 10L};
        Object[] row2 = new Object[]{2026, 8, BigDecimal.valueOf(8000000), 15L};

        when(orderRepository.getRevenueGroupedByMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row1, row2));

        RevenueReportResponse response = reportService.getRevenueReport(fromDate, toDate, ReportGroupBy.MONTH);

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("13000000");
        assertThat(response.getTotalCompletedOrders()).isEqualTo(25L);
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getPeriod()).isEqualTo("2026-07");
        assertThat(response.getItems().get(1).getPeriod()).isEqualTo("2026-08");
    }

    @Test
    @DisplayName("Báo cáo doanh thu gom nhóm theo Năm (YEAR) thành công")
    void getRevenueReport_Success_GroupedByYear() {
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 12, 31);

        Object[] row1 = new Object[]{2025, BigDecimal.valueOf(50000000), 100L};
        Object[] row2 = new Object[]{2026, BigDecimal.valueOf(80000000), 160L};

        when(orderRepository.getRevenueGroupedByYear(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row1, row2));

        RevenueReportResponse response = reportService.getRevenueReport(fromDate, toDate, ReportGroupBy.YEAR);

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("130000000");
        assertThat(response.getItems().get(0).getPeriod()).isEqualTo("2025");
        assertThat(response.getItems().get(1).getPeriod()).isEqualTo("2026");
    }

    @Test
    @DisplayName("Ném 400 Bad Request khi fromDate lớn hơn toDate")
    void getRevenueReport_Throws400_WhenFromDateAfterToDate() {
        LocalDate fromDate = LocalDate.of(2026, 8, 20);
        LocalDate toDate = LocalDate.of(2026, 8, 10);

        assertThatThrownBy(() -> reportService.getRevenueReport(fromDate, toDate, ReportGroupBy.DAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fromDate không được lớn hơn toDate");
    }

    @Test
    @DisplayName("Lấy danh sách top sản phẩm bán chạy nhất thành công qua JPQL GROUP BY")
    void getTopSellingProducts_Success() {
        Object[] row1 = new Object[]{100L, "Bình giữ nhiệt Eco", 50L, BigDecimal.valueOf(10000000)};
        Object[] row2 = new Object[]{101L, "Túi vải Canvas", 30L, BigDecimal.valueOf(3000000)};

        Product p1 = Product.builder().id(100L).name("Bình giữ nhiệt Eco").build();
        Product p2 = Product.builder().id(101L).name("Túi vải Canvas").build();

        when(orderItemRepository.findTopSellingProductsBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(row1, row2));
        when(productRepository.findById(100L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(101L)).thenReturn(Optional.of(p2));

        List<TopSellingProductResponse> result = reportService.getTopSellingProducts(5, null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(100L);
        assertThat(result.get(0).getTotalQuantitySold()).isEqualTo(50L);
        assertThat(result.get(1).getProductId()).isEqualTo(101L);
        assertThat(result.get(1).getTotalQuantitySold()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Lấy tỷ trọng doanh số theo danh mục (Sales by Category) thành công")
    void getSalesByCategory_Success() {
        Object[] row1 = new Object[]{1L, "Gia dụng xanh", 20L, BigDecimal.valueOf(6000000)};
        Object[] row2 = new Object[]{2L, "Thời trang tái chế", 10L, BigDecimal.valueOf(4000000)};

        when(orderItemRepository.getSalesByCategoryBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row1, row2));

        List<CategorySalesResponse> result = reportService.getSalesByCategory(null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryName()).isEqualTo("Gia dụng xanh");
        assertThat(result.get(0).getPercentage()).isEqualTo(60.0);
        assertThat(result.get(1).getCategoryName()).isEqualTo("Thời trang tái chế");
        assertThat(result.get(1).getPercentage()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("Lấy tỷ trọng doanh số theo thương hiệu (Sales by Brand) thành công")
    void getSalesByBrand_Success() {
        Object[] row1 = new Object[]{1L, "EcoLife", 25L, BigDecimal.valueOf(10000000)};

        when(orderItemRepository.getSalesByBrandBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row1));

        List<BrandSalesResponse> result = reportService.getSalesByBrand(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBrandName()).isEqualTo("EcoLife");
        assertThat(result.get(0).getPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Thống kê cơ cấu phương thức thanh toán thành công")
    void getPaymentMethodStats_Success() {
        Object[] row1 = new Object[]{PaymentMethod.COD, 10L, BigDecimal.valueOf(5000000)};
        Object[] row2 = new Object[]{PaymentMethod.VNPAY, 10L, BigDecimal.valueOf(5000000)};

        when(orderRepository.getPaymentMethodDistribution(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row1, row2));

        List<PaymentMethodStatsResponse> result = reportService.getPaymentMethodStats(null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(result.get(0).getPercentage()).isEqualTo(50.0);
        assertThat(result.get(1).getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY);
        assertThat(result.get(1).getPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Thống kê tăng trưởng khách hàng mới thành công")
    void getCustomerGrowth_Success() {
        Object[] row1 = new Object[]{2026, 8, 1, 5L};
        Object[] row2 = new Object[]{2026, 8, 2, 7L};

        when(userRepository.getCustomerGrowthGroupedByDay(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row1, row2));

        CustomerGrowthResponse result = reportService.getCustomerGrowth(null, null, ReportGroupBy.DAY);

        assertThat(result).isNotNull();
        assertThat(result.getTotalNewCustomers()).isEqualTo(12L);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getPeriod()).isEqualTo("2026-08-01");
    }

    @Test
    @DisplayName("Lấy danh sách khách hàng VIP chi tiêu cao nhất thành công")
    void getTopCustomers_Success() {
        Object[] row1 = new Object[]{10L, "Tran Van VIP", "vip@example.com", "0909999999", 5L, BigDecimal.valueOf(25000000)};

        when(orderRepository.findTopCustomers(any(Pageable.class))).thenReturn(List.<Object[]>of(row1));

        List<TopCustomerResponse> result = reportService.getTopCustomers(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Tran Van VIP");
        assertThat(result.get(0).getTotalSpent()).isEqualByComparingTo("25000000");
    }

    @Test
    @DisplayName("Phân tích sức khỏe khách hàng, tỷ lệ mua lại và phát hiện khách hàng rủi ro hủy đơn")
    void getCustomerInsights_Success() {
        when(userRepository.countByRoleName("CUSTOMER")).thenReturn(100L);
        when(userRepository.countByRoleNameAndIsActive("CUSTOMER", true)).thenReturn(90L);
        when(userRepository.countByRoleNameAndIsActive("CUSTOMER", false)).thenReturn(10L);

        Object[] user1 = new Object[]{1L, 3L};
        Object[] user2 = new Object[]{2L, 1L};
        when(orderRepository.countCompletedOrdersPerUser()).thenReturn(List.<Object[]>of(user1, user2));

        Object[] riskUser = new Object[]{3L, "Bad Guy", "bad@example.com", "0911222333", 4L, 3L};
        when(orderRepository.findCustomerOrderAndCancelStats()).thenReturn(List.<Object[]>of(riskUser));

        CustomerInsightsResponse result = reportService.getCustomerInsights();

        assertThat(result).isNotNull();
        assertThat(result.getTotalCustomers()).isEqualTo(100L);
        assertThat(result.getActiveCustomers()).isEqualTo(90L);
        assertThat(result.getPayingCustomers()).isEqualTo(2L);
        assertThat(result.getRepeatCustomers()).isEqualTo(1L);
        assertThat(result.getRepeatPurchaseRate()).isEqualTo(50.0);
        assertThat(result.getHighRiskCustomers()).hasSize(1);
        assertThat(result.getHighRiskCustomers().get(0).getFullName()).isEqualTo("Bad Guy");
        assertThat(result.getHighRiskCustomers().get(0).getCancellationRate()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Lấy danh sách cảnh báo tồn kho và sản phẩm hết hàng thành công")
    void getInventoryAlerts_Success() {
        Object[] lowStock = new Object[]{1L, "Sữa tắm organic", "Chăm sóc cá nhân", 2, BigDecimal.valueOf(150000)};
        Object[] outOfStock = new Object[]{2L, "Ống hút tre", "Gia dụng", 0, BigDecimal.valueOf(30000)};

        when(inventoryRepository.findLowStockProductsDetailed(5)).thenReturn(List.<Object[]>of(lowStock, outOfStock));

        List<InventoryAlertResponse> result = reportService.getInventoryAlerts(5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isOutOfStock()).isFalse();
        assertThat(result.get(1).isOutOfStock()).isTrue();
    }

    @Test
    @DisplayName("Báo cáo chất lượng đánh giá, CSAT và cảnh báo review 1-2 sao thành công")
    void getReviewInsights_Success() {
        when(reviewRepository.count()).thenReturn(10L);
        when(reviewRepository.countByIsVisible(true)).thenReturn(9L);
        when(reviewRepository.countByIsVisible(false)).thenReturn(1L);
        when(reviewRepository.getAverageRatingPlatform()).thenReturn(4.5);

        when(reviewRepository.countByRating(1)).thenReturn(1L);
        when(reviewRepository.countByRating(2)).thenReturn(0L);
        when(reviewRepository.countByRating(3)).thenReturn(1L);
        when(reviewRepository.countByRating(4)).thenReturn(3L);
        when(reviewRepository.countByRating(5)).thenReturn(5L);

        Review badReview = Review.builder()
                .id(100L)
                .rating(1)
                .comment("Hàng bị móp méo khi nhận")
                .createdAt(LocalDateTime.now())
                .user(User.builder().fullName("Angry Customer").build())
                .product(Product.builder().id(50L).name("Bình giữ nhiệt").build())
                .build();
        when(reviewRepository.findTop5ByRatingLessThanEqualAndIsVisibleTrueOrderByCreatedAtDesc(2))
                .thenReturn(List.of(badReview));

        Object[] lowestRow = new Object[]{50L, "Bình giữ nhiệt", 2.5, 4L};
        when(reviewRepository.findLowestRatedProducts(any(Pageable.class))).thenReturn(List.<Object[]>of(lowestRow));

        ReviewInsightsResponse result = reportService.getReviewInsights();

        assertThat(result).isNotNull();
        assertThat(result.getTotalReviews()).isEqualTo(10L);
        assertThat(result.getAveragePlatformRating()).isEqualTo(4.5);
        assertThat(result.getSatisfactionRate()).isEqualTo(80.0);
        assertThat(result.getRecentCriticalReviews()).hasSize(1);
        assertThat(result.getRecentCriticalReviews().get(0).getCustomerName()).isEqualTo("Angry Customer");
        assertThat(result.getLowestRatedProducts()).hasSize(1);
        assertThat(result.getLowestRatedProducts().get(0).getAverageRating()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("Thống kê tác động xanh (Eco-Impact) thành công")
    void getEcoImpact_Success() {
        Object[] row = new Object[]{4.8, 150L};
        when(orderItemRepository.getEcoImpactSummary()).thenReturn(List.<Object[]>of(row));

        EcoImpactResponse result = reportService.getEcoImpact();

        assertThat(result).isNotNull();
        assertThat(result.getAverageEcoScore()).isEqualTo(4.8);
        assertThat(result.getHighEcoScoreProductsSold()).isEqualTo(150L);
    }
}
