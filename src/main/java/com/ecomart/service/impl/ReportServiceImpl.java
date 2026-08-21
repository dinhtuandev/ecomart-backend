package com.ecomart.service.impl;

import com.ecomart.dto.response.*;
import com.ecomart.entity.*;
import com.ecomart.entity.enums.ContactStatus;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.ReportGroupBy;
import com.ecomart.exception.BadRequestException;
import com.ecomart.repository.*;
import com.ecomart.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        long totalOrders = orderRepository.count();
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        long totalCustomers = userRepository.countByRoleName("CUSTOMER");
        long totalProducts = productRepository.count();
        long lowStockProducts = inventoryRepository.countByQuantityLessThanEqual(5);
        long newContactMessages = contactMessageRepository.countByStatus(ContactStatus.NEW);

        List<Order> recentOrdersList = orderRepository.findTop5ByOrderByOrderedAtDesc();
        List<OrderResponse> recentOrders = recentOrdersList.stream()
                .map(this::mapToOrderResponse)
                .toList();

        return DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .cancelledOrders(cancelledOrders)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .newContactMessages(newContactMessages)
                .recentOrders(recentOrders)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate, ReportGroupBy groupBy) {
        if (fromDate == null) {
            fromDate = LocalDate.now().minusDays(30);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (groupBy == null) {
            groupBy = ReportGroupBy.DAY;
        }

        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate không được lớn hơn toDate");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        List<RevenuePeriodData> items = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalCompletedOrders = 0L;

        if (groupBy == ReportGroupBy.DAY) {
            List<Object[]> rows = orderRepository.getRevenueGroupedByDay(start, end);
            for (Object[] row : rows) {
                Integer year = ((Number) row[0]).intValue();
                Integer month = ((Number) row[1]).intValue();
                Integer day = ((Number) row[2]).intValue();
                BigDecimal rev = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
                Long count = row[4] != null ? ((Number) row[4]).longValue() : 0L;

                String period = String.format("%04d-%02d-%02d", year, month, day);
                items.add(new RevenuePeriodData(period, rev, count));

                totalRevenue = totalRevenue.add(rev);
                totalCompletedOrders += count;
            }
        } else if (groupBy == ReportGroupBy.MONTH) {
            List<Object[]> rows = orderRepository.getRevenueGroupedByMonth(start, end);
            for (Object[] row : rows) {
                Integer year = ((Number) row[0]).intValue();
                Integer month = ((Number) row[1]).intValue();
                BigDecimal rev = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                Long count = row[3] != null ? ((Number) row[3]).longValue() : 0L;

                String period = String.format("%04d-%02d", year, month);
                items.add(new RevenuePeriodData(period, rev, count));

                totalRevenue = totalRevenue.add(rev);
                totalCompletedOrders += count;
            }
        } else {
            List<Object[]> rows = orderRepository.getRevenueGroupedByYear(start, end);
            for (Object[] row : rows) {
                Integer year = ((Number) row[0]).intValue();
                BigDecimal rev = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                Long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;

                String period = String.valueOf(year);
                items.add(new RevenuePeriodData(period, rev, count));

                totalRevenue = totalRevenue.add(rev);
                totalCompletedOrders += count;
            }
        }

        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalCompletedOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalCompletedOrders), 2, RoundingMode.HALF_UP);
        }

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalCompletedOrders(totalCompletedOrders)
                .averageOrderValue(averageOrderValue)
                .groupBy(groupBy)
                .fromDate(fromDate)
                .toDate(toDate)
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopSellingProductResponse> getTopSellingProducts(int limit, LocalDate fromDate, LocalDate toDate) {
        if (limit <= 0) {
            limit = 5;
        }
        if (limit > 20) {
            limit = 20;
        }
        if (fromDate == null) {
            fromDate = LocalDate.now().minusDays(30);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }

        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate không được lớn hơn toDate");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> rows = orderItemRepository.findTopSellingProductsBetween(start, end, pageable);

        List<TopSellingProductResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            Long totalQuantitySold = ((Number) row[2]).longValue();
            BigDecimal productRevenue = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            String primaryImageUrl = null;
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
                primaryImageUrl = product.getImages().stream()
                        .filter(ProductImage::isPrimary)
                        .findFirst()
                        .map(ProductImage::getImageUrl)
                        .orElseGet(() -> product.getImages().get(0).getImageUrl());
            }

            result.add(TopSellingProductResponse.builder()
                    .productId(productId)
                    .productName(productName)
                    .productImageUrl(primaryImageUrl)
                    .totalQuantitySold(totalQuantitySold)
                    .totalRevenue(productRevenue)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySalesResponse> getSalesByCategory(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) {
            fromDate = LocalDate.now().minusDays(30);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate không được lớn hơn toDate");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        List<Object[]> rows = orderItemRepository.getSalesByCategoryBetween(start, end);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Object[] row : rows) {
            BigDecimal rev = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            totalRevenue = totalRevenue.add(rev);
        }

        List<CategorySalesResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long categoryId = ((Number) row[0]).longValue();
            String categoryName = (String) row[1];
            Long quantitySold = ((Number) row[2]).longValue();
            BigDecimal rev = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            double percentage = 0.0;
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                percentage = rev.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            result.add(CategorySalesResponse.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .quantitySold(quantitySold)
                    .revenue(rev)
                    .percentage(percentage)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandSalesResponse> getSalesByBrand(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) {
            fromDate = LocalDate.now().minusDays(30);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate không được lớn hơn toDate");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        List<Object[]> rows = orderItemRepository.getSalesByBrandBetween(start, end);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Object[] row : rows) {
            BigDecimal rev = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            totalRevenue = totalRevenue.add(rev);
        }

        List<BrandSalesResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long brandId = ((Number) row[0]).longValue();
            String brandName = (String) row[1];
            Long quantitySold = ((Number) row[2]).longValue();
            BigDecimal rev = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            double percentage = 0.0;
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                percentage = rev.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            result.add(BrandSalesResponse.builder()
                    .brandId(brandId)
                    .brandName(brandName)
                    .quantitySold(quantitySold)
                    .revenue(rev)
                    .percentage(percentage)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodStatsResponse> getPaymentMethodStats(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) {
            fromDate = LocalDate.now().minusDays(30);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate không được lớn hơn toDate");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        List<Object[]> rows = orderRepository.getPaymentMethodDistribution(start, end);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Object[] row : rows) {
            BigDecimal amount = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            totalAmount = totalAmount.add(amount);
        }

        List<PaymentMethodStatsResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            PaymentMethod method = (PaymentMethod) row[0];
            Long orderCount = ((Number) row[1]).longValue();
            BigDecimal amount = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;

            double percentage = 0.0;
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            result.add(PaymentMethodStatsResponse.builder()
                    .paymentMethod(method)
                    .orderCount(orderCount)
                    .totalAmount(amount)
                    .percentage(percentage)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerGrowthResponse getCustomerGrowth(LocalDate fromDate, LocalDate toDate, ReportGroupBy groupBy) {
        if (fromDate == null) {
            fromDate = LocalDate.now().minusDays(30);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (groupBy == null) {
            groupBy = ReportGroupBy.DAY;
        }

        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate không được lớn hơn toDate");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        List<CustomerGrowthData> items = new ArrayList<>();
        long totalNew = 0L;

        if (groupBy == ReportGroupBy.MONTH) {
            List<Object[]> rows = userRepository.getCustomerGrowthGroupedByMonth(start, end);
            for (Object[] row : rows) {
                Integer year = ((Number) row[0]).intValue();
                Integer month = ((Number) row[1]).intValue();
                Long count = ((Number) row[2]).longValue();

                String period = String.format("%04d-%02d", year, month);
                items.add(new CustomerGrowthData(period, count));
                totalNew += count;
            }
        } else {
            List<Object[]> rows = userRepository.getCustomerGrowthGroupedByDay(start, end);
            for (Object[] row : rows) {
                Integer year = ((Number) row[0]).intValue();
                Integer month = ((Number) row[1]).intValue();
                Integer day = ((Number) row[2]).intValue();
                Long count = ((Number) row[3]).longValue();

                String period = String.format("%04d-%02d-%02d", year, month, day);
                items.add(new CustomerGrowthData(period, count));
                totalNew += count;
            }
        }

        return CustomerGrowthResponse.builder()
                .totalNewCustomers(totalNew)
                .groupBy(groupBy)
                .fromDate(fromDate)
                .toDate(toDate)
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopCustomerResponse> getTopCustomers(int limit) {
        if (limit <= 0) {
            limit = 5;
        }
        if (limit > 20) {
            limit = 20;
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> rows = orderRepository.findTopCustomers(pageable);

        List<TopCustomerResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            String fullName = (String) row[1];
            String email = (String) row[2];
            String phone = (String) row[3];
            Long completedOrdersCount = ((Number) row[4]).longValue();
            BigDecimal totalSpent = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;

            result.add(TopCustomerResponse.builder()
                    .userId(userId)
                    .fullName(fullName)
                    .email(email)
                    .phoneNumber(phone)
                    .completedOrdersCount(completedOrdersCount)
                    .totalSpent(totalSpent)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerInsightsResponse getCustomerInsights() {
        long totalCustomers = userRepository.countByRoleName("CUSTOMER");
        long activeCustomers = userRepository.countByRoleNameAndIsActive("CUSTOMER", true);
        long inactiveCustomers = userRepository.countByRoleNameAndIsActive("CUSTOMER", false);

        List<Object[]> userOrders = orderRepository.countCompletedOrdersPerUser();
        long payingCustomers = userOrders.size();
        long repeatCustomers = userOrders.stream()
                .filter(row -> ((Number) row[1]).longValue() >= 2)
                .count();

        double repeatPurchaseRate = 0.0;
        if (payingCustomers > 0) {
            repeatPurchaseRate = BigDecimal.valueOf((double) repeatCustomers * 100.0 / payingCustomers)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        List<Object[]> riskRows = orderRepository.findCustomerOrderAndCancelStats();
        List<HighRiskCustomerData> highRiskList = new ArrayList<>();
        for (Object[] row : riskRows) {
            Long userId = ((Number) row[0]).longValue();
            String fullName = (String) row[1];
            String email = (String) row[2];
            String phone = (String) row[3];
            Long total = ((Number) row[4]).longValue();
            Long cancelled = ((Number) row[5]).longValue();

            double cancelRate = total > 0 ? (cancelled * 100.0 / total) : 0.0;
            cancelRate = BigDecimal.valueOf(cancelRate).setScale(2, RoundingMode.HALF_UP).doubleValue();

            if (cancelled >= 3 || (total >= 3 && cancelRate >= 50.0)) {
                highRiskList.add(HighRiskCustomerData.builder()
                        .userId(userId)
                        .fullName(fullName)
                        .email(email)
                        .phoneNumber(phone)
                        .totalOrders(total)
                        .cancelledOrders(cancelled)
                        .cancellationRate(cancelRate)
                        .build());
            }
        }

        return CustomerInsightsResponse.builder()
                .totalCustomers(totalCustomers)
                .activeCustomers(activeCustomers)
                .inactiveCustomers(inactiveCustomers)
                .payingCustomers(payingCustomers)
                .repeatCustomers(repeatCustomers)
                .repeatPurchaseRate(repeatPurchaseRate)
                .highRiskCustomers(highRiskList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAlertResponse> getInventoryAlerts(Integer threshold) {
        if (threshold == null || threshold <= 0) {
            threshold = 5;
        }

        List<Object[]> rows = inventoryRepository.findLowStockProductsDetailed(threshold);
        List<InventoryAlertResponse> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            String categoryName = (String) row[2];
            Integer currentStock = row[3] != null ? ((Number) row[3]).intValue() : 0;
            BigDecimal sellingPrice = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;

            result.add(InventoryAlertResponse.builder()
                    .productId(productId)
                    .productName(productName)
                    .categoryName(categoryName)
                    .currentStock(currentStock)
                    .sellingPrice(sellingPrice)
                    .isOutOfStock(currentStock <= 0)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewInsightsResponse getReviewInsights() {
        long totalReviews = reviewRepository.count();
        long visibleReviews = reviewRepository.countByIsVisible(true);
        long hiddenReviews = reviewRepository.countByIsVisible(false);

        Double avgRating = reviewRepository.getAverageRatingPlatform();
        double averagePlatformRating = avgRating != null ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        Map<Integer, Long> ratingDistribution = new LinkedHashMap<>();
        long count4And5 = 0L;
        for (int i = 1; i <= 5; i++) {
            long count = reviewRepository.countByRating(i);
            ratingDistribution.put(i, count);
            if (i >= 4) {
                count4And5 += count;
            }
        }

        double satisfactionRate = 0.0;
        if (totalReviews > 0) {
            satisfactionRate = BigDecimal.valueOf(count4And5 * 100.0 / totalReviews).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        List<Review> criticalReviews = reviewRepository.findTop5ByRatingLessThanEqualAndIsVisibleTrueOrderByCreatedAtDesc(2);
        List<CriticalReviewAlertData> criticalList = criticalReviews.stream()
                .map(r -> CriticalReviewAlertData.builder()
                        .reviewId(r.getId())
                        .productId(r.getProduct() != null ? r.getProduct().getId() : null)
                        .productName(r.getProduct() != null ? r.getProduct().getName() : null)
                        .customerName(r.getUser() != null ? r.getUser().getFullName() : null)
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        List<Object[]> lowestRows = reviewRepository.findLowestRatedProducts(PageRequest.of(0, 5));
        List<ProductRatingSummaryData> lowestProducts = new ArrayList<>();
        for (Object[] row : lowestRows) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            Double avg = row[2] != null ? BigDecimal.valueOf(((Number) row[2]).doubleValue()).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;
            Long reviewCount = ((Number) row[3]).longValue();

            lowestProducts.add(ProductRatingSummaryData.builder()
                    .productId(productId)
                    .productName(productName)
                    .averageRating(avg)
                    .totalReviews(reviewCount)
                    .build());
        }

        return ReviewInsightsResponse.builder()
                .totalReviews(totalReviews)
                .visibleReviews(visibleReviews)
                .hiddenReviews(hiddenReviews)
                .averagePlatformRating(averagePlatformRating)
                .satisfactionRate(satisfactionRate)
                .ratingDistribution(ratingDistribution)
                .recentCriticalReviews(criticalList)
                .lowestRatedProducts(lowestProducts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EcoImpactResponse getEcoImpact() {
        List<Object[]> rows = orderItemRepository.getEcoImpactSummary();
        if (rows.isEmpty() || rows.get(0) == null) {
            return EcoImpactResponse.builder()
                    .averageEcoScore(0.0)
                    .certifiedProductsSold(0L)
                    .highEcoScoreProductsSold(0L)
                    .build();
        }

        Object[] row = rows.get(0);
        Double avgEco = row[0] != null ? BigDecimal.valueOf(((Number) row[0]).doubleValue()).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;
        Long highEcoSold = row[1] != null ? ((Number) row[1]).longValue() : 0L;

        return EcoImpactResponse.builder()
                .averageEcoScore(avgEco)
                .certifiedProductsSold(highEcoSold)
                .highEcoScoreProductsSold(highEcoSold)
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String primaryImageUrl = null;
                Product p = item.getProduct();
                if (p != null && p.getImages() != null && !p.getImages().isEmpty()) {
                    primaryImageUrl = p.getImages().stream()
                            .filter(ProductImage::isPrimary)
                            .findFirst()
                            .map(ProductImage::getImageUrl)
                            .orElseGet(() -> p.getImages().get(0).getImageUrl());
                }

                itemResponses.add(OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(p != null ? p.getId() : null)
                        .productName(item.getProductName())
                        .productImageUrl(primaryImageUrl)
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build());
            }
        }

        List<PaymentTransactionResponse> txResponses = new ArrayList<>();
        if (order.getPaymentTransactions() != null) {
            for (PaymentTransaction tx : order.getPaymentTransactions()) {
                txResponses.add(PaymentTransactionResponse.builder()
                        .id(tx.getId())
                        .paymentRef(tx.getPaymentRef())
                        .gateway(tx.getGateway() != null ? tx.getGateway().name() : null)
                        .amount(tx.getAmount())
                        .gatewayTransactionNo(tx.getGatewayTransactionNo())
                        .status(tx.getStatus() != null ? tx.getStatus().name() : null)
                        .createdAt(tx.getCreatedAt())
                        .build());
            }
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .cancellationReason(order.getCancellationReason())
                .orderedAt(order.getOrderedAt())
                .confirmedAt(order.getConfirmedAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .paidAt(order.getPaidAt())
                .items(itemResponses)
                .paymentTransactions(txResponses)
                .build();
    }
}
